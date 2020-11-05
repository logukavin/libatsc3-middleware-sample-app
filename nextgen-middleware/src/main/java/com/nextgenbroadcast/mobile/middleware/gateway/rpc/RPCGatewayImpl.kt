package com.nextgenbroadcast.mobile.middleware.gateway.rpc

import androidx.lifecycle.distinctUntilChanged
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.cache.IApplicationCache
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.rpc.notification.RPCNotifier
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.Urls
import com.nextgenbroadcast.mobile.middleware.server.ws.MiddlewareWebSocket
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

internal class RPCGatewayImpl(
        private val serviceController: IServiceController,
        private val viewController: IViewController,
        private val applicationCache: IApplicationCache,
        settings: IMiddlewareSettings,
        mainDispatcher: CoroutineDispatcher,
        ioDispatcher: CoroutineDispatcher
) : IRPCGateway {
    private val mainScope = CoroutineScope(mainDispatcher)
    private val ioScope = CoroutineScope(ioDispatcher)

    private val sessions = CopyOnWriteArrayList<MiddlewareWebSocket>()
    private val subscribedNotifications = mutableSetOf<NotificationType>()
    private val rpcNotifier = RPCNotifier(this)

    override val deviceId = settings.deviceId
    override val advertisingId = settings.advertisingId
    override val language: String = Locale.getDefault().language
    override val queryServiceId: String?
        get() = serviceController.selectedService.value?.globalId
    override val mediaUrl: String?
        get() = viewController.rmpMediaUri.value.toString()
    override val playbackState: PlaybackState
        get() = viewController.rmpState.value ?: PlaybackState.IDLE
    override val serviceGuideUrls: List<Urls>
        get() = serviceController.serviceGuidUrls.value ?: emptyList()

    private val rmpPlaybackTime: Long
        get() = viewController.rmpMediaTime.value ?: 0

    private var currentAppData: AppData? = null
    private var mediaTimeUpdateJob: Job? = null
    private val currentAppContextId: String?
        get() = currentAppData?.appContextId

    init {
        viewController.appData.distinctUntilChanged().observeForever { appData ->
            onAppDataUpdated(appData)
        }

        serviceController.serviceGuidUrls.observeForever { urls ->
            onServiceGuidUrls(urls)
        }

        viewController.rmpMediaUri.distinctUntilChanged().observeForever {
            onMediaUrlUpdated()
        }

        viewController.rmpState.distinctUntilChanged().observeForever { playbackState ->
            onRMPPlaybackStateChanged(playbackState)
        }

        viewController.rmpPlaybackRate.distinctUntilChanged().observeForever { playbackRate ->
            onRMPPlaybackRateChanged(playbackRate)
        }
    }

    override fun onSocketOpened(socket: MiddlewareWebSocket) {
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

    override fun addFilesToCache(baseUrl: String?, rootPath: String?, paths: List<String>, filters: List<String>?): Boolean {
        val requestFileCache = currentAppContextId?.let { appContextId ->
            applicationCache.requestFiles(appContextId, rootPath, baseUrl, paths, filters )
        } ?: false

        return requestFileCache
    }

    private fun onAppDataUpdated(appData: AppData?) {
        currentAppContextId?.let { applicationCache.clearCache(it) }

        // Notify the user changes to another service also associated with the same Broadcaster Application
        appData?.let {
            if (appData.isAppEquals(currentAppData)) {
                onApplicationServiceChanged(appData.appContextId)
            }
        }
        currentAppData = appData
    }

    private fun onApplicationServiceChanged(serviceId: String) {
        if (subscribedNotifications.contains(NotificationType.SERVICE_CHANGE)) {
            rpcNotifier.notifyServiceChange(serviceId)
        }
    }

    private fun onServiceGuidUrls(urls: List<Urls>?) {
        if (subscribedNotifications.contains(NotificationType.SERVICE_GUIDE_CHANGE)) {
            urls?.let { it -> rpcNotifier.notifyServiceGuideChange(it) }
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

    companion object {
        val SUPPORTED_NOTIFICATIONS = setOf(
                NotificationType.SERVICE_CHANGE,
                NotificationType.SERVICE_GUIDE_CHANGE,
                NotificationType.MPD_CHANGE,
                NotificationType.RMP_PLAYBACK_STATE_CHANGE,
                NotificationType.RMP_PLAYBACK_RATE_CHANGE,
                NotificationType.RMP_MEDIA_TIME_CHANGE
        )

        private const val MEDIA_TIME_UPDATE_DELAY = 500L
    }
}