package org.ngbp.jsonrpc4jtestharness.gateway.rpc

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.ngbp.jsonrpc4jtestharness.controller.service.IServiceController
import org.ngbp.jsonrpc4jtestharness.controller.view.IViewController
import org.ngbp.jsonrpc4jtestharness.core.model.AppData
import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.core.repository.IRepository
import org.ngbp.jsonrpc4jtestharness.core.ws.SocketHolder
import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType
import org.ngbp.jsonrpc4jtestharness.rpc.notification.RPCNotifier
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Urls
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RPCGatewayImpl @Inject constructor(
        private val serviceController: IServiceController,
        private val viewController: IViewController,
        private val repository: IRepository,
        private val socketHolder: SocketHolder
) : IRPCGateway {
    private val mainScope = MainScope()
    private val subscribedNotifications = mutableSetOf<NotificationType>()
    private val rpcNotifier = RPCNotifier(this)

    private var currentAppData: AppData? = null

    override val language: String = java.util.Locale.getDefault().language
    override val queryServiceId: String?
        get() = serviceController.selectedService.value?.globalId
    override val mediaUrl: String?
        get() = viewController.rmpMediaUrl.value
    override val playbackState: PlaybackState
        get() = viewController.rmpState.value ?: PlaybackState.IDLE
    override val serviceGuideUrls: List<Urls>
        get() = serviceController.serviceGuidUrls.value ?: emptyList()
    override val playbackRate: Float
        get() = viewController.rmpPlaybackRate.value ?: 1F
    override val mediaTime: Double
        get() = viewController.rmpMediaTime.value ?: 0.0

    init {
        repository.appData.observeForever{ appData ->
            onAppDataUpdated(appData)
        }

        serviceController.serviceGuidUrls.observeForever { urls ->
            onServiceGuidUrls(urls)
        }

        viewController.rmpMediaUrl.observeForever { onMediaUrlUpdated() }

        viewController.rmpState.observeForever { playbackState ->
            onRMPPlaybackStateChanged(playbackState)
        }

        viewController.rmpPlaybackRate.observeForever { playbackRate ->
            onRMPPlaybackRateChanged(playbackRate)
        }

        viewController.rmpMediaTime.observeForever { mediaTime ->
            onMediaTimeChanged(mediaTime)
        }
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
        return available
    }

    override fun unsubscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType> {
        val available = getAvailableNotifications(notifications)
        subscribedNotifications.removeAll(available)
        return available
    }

    private fun getAvailableNotifications(requested: Set<NotificationType>): Set<NotificationType> {
        val available = SUPPORTED_NOTIFICATIONS.toMutableSet()
        available.retainAll(requested)
        return available
    }

    override fun sendNotification(message: String) {
        socketHolder.broadcastMessage(message)
    }

    private fun onAppDataUpdated(appData: AppData?) {
        appData?.let {
            if (appData.isAppEquals(currentAppData)) {
                if (subscribedNotifications.contains(NotificationType.SERVICE_CHANGE)) {
                    rpcNotifier.notifyServiceChange(serviceId = it.appContextId)
                }
            }
        }
        currentAppData = appData
    }

    private fun onServiceGuidUrls(urls: List<Urls>?) {
        if (subscribedNotifications.contains(NotificationType.SERVICE_GUIDE_CHANGE)) {
            urls?.let { it -> rpcNotifier.notifyServiceGuideChange(urlList = it) }
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
    }

    private fun onRMPPlaybackRateChanged(playbackRate: Float) {
        if (subscribedNotifications.contains(NotificationType.RMP_PLAYBACK_RATE_CHANGE)) {
            rpcNotifier.notifyRmpPlaybackRateChange(playbackRate)
        }
    }

    private fun onMediaTimeChanged(mediaTime: Double) {
        if (subscribedNotifications.contains(NotificationType.RMP_MEDIA_TIME_CHANGE)) {
            rpcNotifier.notifyRmpMediaTimeChange(currentTime = mediaTime)
        }
    }

    companion object {
        private val SUPPORTED_NOTIFICATIONS = setOf(
                NotificationType.SERVICE_CHANGE,
                NotificationType.SERVICE_GUIDE_CHANGE,
                NotificationType.MPD_CHANGE,
                NotificationType.RMP_PLAYBACK_STATE_CHANGE,
                NotificationType.RMP_PLAYBACK_RATE_CHANGE,
                NotificationType.RMP_MEDIA_TIME_CHANGE
        )
    }
}