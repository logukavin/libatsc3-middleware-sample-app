package com.nextgenbroadcast.mobile.middleware.gateway.rpc

import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.atsc3.ISignalingData
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts.AeaTable
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3ApplicationFile
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl
import com.nextgenbroadcast.mobile.middleware.cache.IApplicationCache
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.server.IApplicationSession
import com.nextgenbroadcast.mobile.middleware.server.ServerUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import java.util.concurrent.CopyOnWriteArrayList

internal class RPCGatewayImpl(
    private val repository: IRepository,
    private val viewController: IViewController,
    private val serviceController: IServiceController,
    private val applicationCache: IApplicationCache,
    private val settings: IMiddlewareSettings,
    private val stateScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : IRPCGateway {
    private val serviceGuideUrls = HashSet<SGUrl>()
    private val appFiles = mutableListOf<Atsc3ApplicationFile>()

    private val sessions = CopyOnWriteArrayList<IApplicationSession>()
    private var mergedAlerts: List<AeaTable> = emptyList()

    private val selectedService = repository.selectedService

    override val deviceId = settings.deviceId
    override val advertisingId = settings.advertisingId
    override val language: String = settings.locale.language
    override val queryServiceId: String?
        get() = selectedService.value?.globalId
    override val mediaUrl: String?
        get() = repository.routeMediaUrl.value?.url
    override var currentAppBaseUrl: String? = null
    override val playbackState: PlaybackState
        get() = viewController.rmpState.value
    override val playbackTime: Long
        get() = viewController.rmpMediaTime.value

    private var currentAppContextId: String? = null
    private var currentServiceId: String? = null

    init {
        stateScope.launch {
            repository.appData.combine(selectedService) { appData, service ->
                Pair(appData, service)
            }.collect { (appData, service) ->
                if (appData != null && service != null) {
                    onAppDataUpdated(appData, service)
                }
            }
        }

        stateScope.launch {
            repository.serviceGuideUrls.collect { urls ->
                if (!urls.isNullOrEmpty()) {
                    onServiceGuidUrls(urls)
                    serviceGuideUrls.addAll(urls)
                }
            }
        }

        stateScope.launch {
            repository.appData.combine(repository.applications) { appData, applications ->
                if (appData != null) {
                    applications.filter { app ->
                        app.cachePath == appData.cachePath
                    }.flatMap { it.files.values }
                } else {
                    emptyList()
                }
            }.collect { applicationFiles ->
                onApplicationContentChanged(applicationFiles)
            }
        }

        stateScope.launch {
            viewController.rmpState.collect { playbackState ->
                onRMPPlaybackStateChanged(playbackState)
            }
        }

        stateScope.launch {
            viewController.rmpPlaybackRate.collect { playbackRate ->
                onRMPPlaybackRateChanged(playbackRate)
            }
        }

        stateScope.launch {
            repository.alertsForNotify.collect { list ->
                if (list.isNotEmpty()) {
                    val result = list.subtract(mergedAlerts).toList()
                    onAlertingChanged(result)
                    mergedAlerts = list
                }
            }
        }
    }

    override fun registerSession(session: IApplicationSession) {
        sessions.add(session)
    }

    override fun unregisterSession(session: IApplicationSession) {
        sessions.remove(session)
    }

    override fun requestRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double) {
        viewController.requestPlayerLayout(scaleFactor, xPos, yPos)
    }

    //TODO: currently delay not supported and blocked on RPC level
    override fun requestMediaPlay(mediaUrl: String?, delay: Long) {
        viewController.requestPlayerState(PlaybackState.PLAYING, mediaUrl)
    }

    //TODO: currently delay not supported and blocked on RPC level
    override fun requestMediaStop(delay: Long) {
        viewController.requestPlayerState(PlaybackState.IDLE)
    }

    override fun filterAvailableNotifications(requested: Set<NotificationType>): Set<NotificationType> {
        val available = SUPPORTED_NOTIFICATIONS.toMutableSet()
        available.retainAll(requested)
        return available
    }

    private fun sendNotification(type: NotificationType, payload: Any) {
        ioScope.launch {
            sessions.forEach { session ->
                session.notify(type, payload)
            }
        }
    }

    override fun requestFileCache(baseUrl: String?, rootPath: String?, paths: List<String>, filters: List<String>?): Boolean {
        return currentAppContextId?.let { appContextId ->
            applicationCache.requestFiles(appContextId, rootPath, baseUrl, paths, filters)
        } ?: false
    }

    override fun getServiceGuideUrls(service: String?): List<SGUrl> {
        return if (service != null) {
            serviceGuideUrls.filter { url -> url.service == service }
        } else {
            serviceGuideUrls
        }.map {
            it.copy(sgPath = ServerUtils.createUrl(it.sgPath, settings))
        }
    }

    override fun requestServiceChange(globalServiceId: String): Boolean {
        return repository.findServiceBy(globalServiceId)?.let { service ->
            runBlocking {
                serviceController.selectService(service)
            }
        } ?: false
    }

    override fun getAEATChangingList(): List<String> {
        return repository.alertsForNotify.value.map { aeat ->
            aeat.xml
        }
    }

    override fun getSignalingInfo(names: List<String>): List<ISignalingData> {
        return serviceController.getSignalingData(names)
    }

    /**
    Shall be issued by the Receiver to the currently executing
    Broadcaster Application if the user changes to another service also associated with the same
    Broadcaster Application
     */
    private fun onAppDataUpdated(appData: AppData, service: AVService) {
        currentAppContextId = appData.contextId
        currentAppBaseUrl = appData.baseUrl
        currentServiceId = service.globalId?.also { globalServiceId ->
            if (currentServiceId != null && currentServiceId != globalServiceId) {
                if (appData.compatibleServiceIds.contains(service.id)) {
                    onApplicationServiceChanged(globalServiceId)
                }
            }
        }
    }

    private fun hasActiveSessions() = sessions.isNotEmpty()

    private fun onApplicationServiceChanged(serviceId: String) {
        if (hasActiveSessions()) {
            sendNotification(NotificationType.SERVICE_CHANGE, serviceId)
        }
    }

    private fun onApplicationContentChanged(files: Collection<Atsc3ApplicationFile>) {
        if (hasActiveSessions()) {
            val changedFiles = files.subtract(appFiles).map {
                it.contentLocation
            }

            if (changedFiles.isNotEmpty()) {
                sendNotification(NotificationType.CONTENT_CHANGE, changedFiles)
            }
        }

        appFiles.clear()
        appFiles.addAll(files)
    }

    private fun onServiceGuidUrls(urls: List<SGUrl>) {
        if (hasActiveSessions()) {
            val diff = urls.subtract(serviceGuideUrls).map { sgUrl ->
                sgUrl.sgType.toString() to ServerUtils.createUrl(sgUrl.sgPath, settings)
            }
            if (diff.isNotEmpty()) {
                sendNotification(NotificationType.SERVICE_GUIDE_CHANGE, diff)
            }
        }
    }

    private fun onRMPPlaybackStateChanged(playbackState: PlaybackState) {
        if (hasActiveSessions()) {
            sendNotification(NotificationType.RMP_PLAYBACK_STATE_CHANGE, playbackState)
        }
    }

    private fun onRMPPlaybackRateChanged(playbackRate: Float) {
        if (hasActiveSessions()) {
            sendNotification(NotificationType.RMP_PLAYBACK_RATE_CHANGE, playbackRate)
        }
    }

    private fun onAlertingChanged(alertList: List<AeaTable>) {
        if (hasActiveSessions()) {
            //don't dispatch every element, just dispatch the xml payload (e.g. from the first element in the internal table)
            //sendNotification(NotificationType.ALERT_CHANGE, alertList.map { it.xml })
            if(alertList.size > 0) {
                sendNotification(NotificationType.ALERT_CHANGE, alertList.first().xml)
            }
        }
    }

    companion object {
        val SUPPORTED_NOTIFICATIONS = setOf(
                NotificationType.SERVICE_CHANGE,
                NotificationType.SERVICE_GUIDE_CHANGE,
                NotificationType.RMP_PLAYBACK_STATE_CHANGE,
                NotificationType.RMP_PLAYBACK_RATE_CHANGE,
                NotificationType.RMP_MEDIA_TIME_CHANGE,
                NotificationType.CONTENT_CHANGE,
                NotificationType.ALERT_CHANGE
        )
    }
}