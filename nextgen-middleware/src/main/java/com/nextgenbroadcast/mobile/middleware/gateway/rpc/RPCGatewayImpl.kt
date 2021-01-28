package com.nextgenbroadcast.mobile.middleware.gateway.rpc

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.distinctUntilChanged
import com.nextgenbroadcast.mobile.core.mapWith
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.unite
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3ApplicationFile
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl
import com.nextgenbroadcast.mobile.middleware.cache.IApplicationCache
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.rpc.notification.RPCNotificationHelper
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.ServiceGuideUrlsRpcResponse
import com.nextgenbroadcast.mobile.middleware.server.ServerUtils
import com.nextgenbroadcast.mobile.middleware.server.ws.MiddlewareWebSocket
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList

internal class RPCGatewayImpl(
        private val viewController: IViewController,
        private val serviceController: IServiceController,
        private val applicationCache: IApplicationCache,
        private val settings: IMiddlewareSettings,
        mainDispatcher: CoroutineDispatcher,
        ioDispatcher: CoroutineDispatcher
) : IRPCGateway {
    private val mainScope = CoroutineScope(mainDispatcher)
    private val ioScope = CoroutineScope(ioDispatcher)
    private val sessions = CopyOnWriteArrayList<MiddlewareWebSocket>()
    private val rpcNotifier = RPCNotificationHelper(this::sendNotification)
    private val serviceGuideUrls = HashSet<SGUrl>()

    override val deviceId = settings.deviceId
    override val advertisingId = settings.advertisingId
    override val language: String = settings.locale.language
    override val queryServiceId: String?
        get() = serviceController.selectedService.value?.globalId
    override val mediaUrl: String
        get() = viewController.rmpMediaUri.value.toString()
    override val playbackState: PlaybackState
        get() = viewController.rmpState.value ?: PlaybackState.IDLE
    private val rmpPlaybackTime: Long
        get() = viewController.rmpMediaTime.value ?: 0

    private var currentAppContextId: String? = null
    private var currentServiceId: String? = null
    private var mediaTimeUpdateJob: Job? = null

    private val appFiles = mutableListOf<Atsc3ApplicationFile>()
    private val subscribedNotifications = mutableSetOf<NotificationType>()

    fun start(lifecycleOwner: LifecycleOwner) {
        viewController.appData.distinctUntilChanged().unite(serviceController.selectedService).observe(lifecycleOwner) { (appData, service) ->
            if (appData != null && service != null) {
                onAppDataUpdated(appData, service)
            }
        }

        serviceController.serviceGuideUrls.observe(lifecycleOwner) { urls ->
            if(!urls.isNullOrEmpty()) {
                onServiceGuidUrls(urls)
                serviceGuideUrls.addAll(urls)
            }
        }

        viewController.appData.mapWith(serviceController.applications) { (appData, applications) ->
            if (appData != null && applications != null) {
                applications.filter { app ->
                    app.cachePath == appData.cachePath
                }.flatMap { it.files.values }
            } else {
                emptyList()
            }
        }.observe(lifecycleOwner) { applicationFiles ->
            onApplicationContentChanged(applicationFiles)
        }

        viewController.rmpMediaUri.distinctUntilChanged().observe(lifecycleOwner) {
            onMediaUrlUpdated()
        }

        viewController.rmpState.distinctUntilChanged().observe(lifecycleOwner) { playbackState ->
            onRMPPlaybackStateChanged(playbackState)
        }

        viewController.rmpPlaybackRate.distinctUntilChanged().observe(lifecycleOwner) { playbackRate ->
            onRMPPlaybackRateChanged(playbackRate)
        }
    }

    private fun closeAllSessionsAndUnsubscribeNotifications() {
        sessions.onEach {
            it.disconnect(ioScope)
        }.clear()

        unsubscribeNotifications(subscribedNotifications)
    }

    override fun onSocketOpened(socket: MiddlewareWebSocket) {
        closeAllSessionsAndUnsubscribeNotifications()

        sessions.add(socket)
    }

    override fun onSocketClosed(socket: MiddlewareWebSocket) {
        sessions.remove(socket)
    }

    override fun updateRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double) {
        mainScope.launch {
            viewController.updateRMPPosition(scaleFactor, xPos, yPos)
        }
    }

    override fun requestMediaPlay(mediaUrl: String?, delay: Long) {
        mainScope.launch {
            viewController.requestMediaPlay(mediaUrl, delay)
        }
    }

    override fun requestMediaStop(delay: Long) {
        mainScope.launch {
            viewController.requestMediaStop(delay)
        }
    }

    override fun subscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType> {
        val available = getAvailableNotifications(notifications)
        subscribedNotifications.addAll(available)

        if (available.contains(NotificationType.RMP_MEDIA_TIME_CHANGE)) {
            if (playbackState == PlaybackState.PLAYING) {
                startMediaTimeUpdateJob()
            }
        }

        return available
    }

    override fun unsubscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType> {
        val available = getAvailableNotifications(notifications)
        subscribedNotifications.removeAll(available)

        if (available.contains(NotificationType.RMP_MEDIA_TIME_CHANGE)) {
            cancelMediaTimeUpdateJob()
        }

        return available
    }

    private fun getAvailableNotifications(requested: Set<NotificationType>): Set<NotificationType> {
        val available = SUPPORTED_NOTIFICATIONS.toMutableSet()
        available.retainAll(requested)
        return available
    }

    override fun sendNotification(message: String) {
        ioScope.launch {
            sessions.forEach { socket ->
                socket.sendMessage(message)
            }
        }
    }

    override fun requestFileCache(baseUrl: String?, rootPath: String?, paths: List<String>, filters: List<String>?): Boolean {
        return currentAppContextId?.let { appContextId ->
            applicationCache.requestFiles(appContextId, rootPath, baseUrl, paths, filters)
        } ?: false
    }

    override fun getServiceGuideUrls(service: String?): List<ServiceGuideUrlsRpcResponse.Url> {
        val urls: Collection<SGUrl> = if (service != null) {
            serviceGuideUrls.filter { url -> url.service == service }
        } else {
            serviceGuideUrls
        }
        return mapServiceGuideUrls(urls, false)
    }

    override fun requestServiceChange(globalServiceId: String): Boolean {
        return serviceController.findServiceById(globalServiceId)?.let { service ->
            serviceController.selectService(service)
        } ?: false
    }

    /**
    Shall be issued by the Receiver to the currently executing
    Broadcaster Application if the user changes to another service also associated with the same
    Broadcaster Application
     */
    private fun onAppDataUpdated(appData: AppData, service: AVService) {
        currentAppContextId = appData.appContextId
        currentServiceId = service.globalId?.also { globalServiceId ->
            if (currentServiceId != null && currentServiceId != globalServiceId) {
                if (appData.compatibleServiceIds.contains(service.id)) {
                    onApplicationServiceChanged(globalServiceId)
                }
            }
        }
    }

    private fun onApplicationServiceChanged(serviceId: String) {
        if (subscribedNotifications.contains(NotificationType.SERVICE_CHANGE)) {
            rpcNotifier.notifyServiceChange(serviceId)
        }
    }

    private fun onApplicationContentChanged(files: Collection<Atsc3ApplicationFile>) {
        if (subscribedNotifications.contains(NotificationType.CONTENT_CHANGE)) {
            val changedFiles = files.subtract(appFiles).map {
                it.contentLocation
            }

            if (changedFiles.isNotEmpty()) {
                rpcNotifier.notifyContentChange(changedFiles)
            }
        }

        appFiles.clear()
        appFiles.addAll(files)
    }

    private fun onServiceGuidUrls(urls: List<SGUrl>) {
        if (subscribedNotifications.contains(NotificationType.SERVICE_GUIDE_CHANGE)) {
            val diff = urls.subtract(serviceGuideUrls)
            if (diff.isNotEmpty()) {
                rpcNotifier.notifyServiceGuideChange(mapServiceGuideUrls(diff, true))
            }
        }
    }

    private fun onMediaUrlUpdated() {
        if (subscribedNotifications.contains(NotificationType.MPD_CHANGE)) {
            rpcNotifier.notifyMPDChange()
        }
    }

    private fun onRMPPlaybackStateChanged(playbackState: PlaybackState) {
        if (subscribedNotifications.contains(NotificationType.RMP_PLAYBACK_STATE_CHANGE)) {
            rpcNotifier.notifyRmpPlaybackStateChange(playbackState)
        }

        if (playbackState == PlaybackState.PLAYING) {
            startMediaTimeUpdateJob()
        } else {
            cancelMediaTimeUpdateJob()
        }
    }

    private fun onRMPPlaybackRateChanged(playbackRate: Float) {
        if (subscribedNotifications.contains(NotificationType.RMP_PLAYBACK_RATE_CHANGE)) {
            rpcNotifier.notifyRmpPlaybackRateChange(playbackRate)
        }
    }

    private fun onMediaTimeChanged(mediaTime: Long) {
        if (subscribedNotifications.contains(NotificationType.RMP_MEDIA_TIME_CHANGE)) {
            rpcNotifier.notifyRmpMediaTimeChange(mediaTime)
        }
    }

    private fun startMediaTimeUpdateJob() {
        cancelMediaTimeUpdateJob()

        if (!subscribedNotifications.contains(NotificationType.RMP_MEDIA_TIME_CHANGE)) return

        mediaTimeUpdateJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                onMediaTimeChanged(rmpPlaybackTime)

                delay(MEDIA_TIME_UPDATE_DELAY)
            }
            mediaTimeUpdateJob = null
        }
    }

    private fun cancelMediaTimeUpdateJob() {
        mediaTimeUpdateJob?.let {
            it.cancel()
            mediaTimeUpdateJob = null
        }
    }

    private fun mapServiceGuideUrls(sgUrls: Collection<SGUrl>, skipContent: Boolean): List<ServiceGuideUrlsRpcResponse.Url> {
        return sgUrls.map { sgUrl ->
            ServiceGuideUrlsRpcResponse.Url(
                    sgUrl.sgType.toString(),
                    ServerUtils.createUrl(sgUrl.sgPath, settings),
                    if (skipContent) null else sgUrl.service,
                    if (skipContent) null else sgUrl.content
            )
        }
    }

    companion object {
        val SUPPORTED_NOTIFICATIONS = setOf(
                NotificationType.SERVICE_CHANGE,
                NotificationType.SERVICE_GUIDE_CHANGE,
                NotificationType.MPD_CHANGE,
                NotificationType.RMP_PLAYBACK_STATE_CHANGE,
                NotificationType.RMP_PLAYBACK_RATE_CHANGE,
                NotificationType.RMP_MEDIA_TIME_CHANGE,
                NotificationType.CONTENT_CHANGE
        )

        private const val MEDIA_TIME_UPDATE_DELAY = 500L
    }
}