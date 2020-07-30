package org.ngbp.jsonrpc4jtestharness.gateway.rpc

import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.*
import org.ngbp.jsonrpc4jtestharness.controller.service.IServiceController
import org.ngbp.jsonrpc4jtestharness.controller.view.IViewController
import org.ngbp.jsonrpc4jtestharness.core.model.AppData
import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.core.repository.IRepository
import org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWebSocket
import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType
import org.ngbp.jsonrpc4jtestharness.rpc.notification.RPCNotifier
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Urls
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RPCGatewayImpl @Inject constructor(
        private val serviceController: IServiceController,
        private val viewController: IViewController,
        repository: IRepository
) : IRPCGateway {
    private val mainScope = MainScope()

    private val sessions = CopyOnWriteArrayList<MiddlewareWebSocket>()
    private val subscribedNotifications = mutableSetOf<NotificationType>()
    private val rpcNotifier = RPCNotifier(this)

    private var currentAppData: AppData? = null
    private var mediaTimeUpdateJob: Job? = null

    override val language: String = java.util.Locale.getDefault().language
    override val queryServiceId: String?
            get() = serviceController.selectedService.value?.globalId
    override val mediaUrl: String?
        get() = viewController.rmpMediaUrl.value
    override val playbackState: PlaybackState
        get() = viewController.rmpState.value ?: PlaybackState.IDLE
    override val serviceGuideUrls: List<Urls>
        get() = serviceController.serviceGuidUrls.value ?: emptyList()

    private val rmpPlaybackTime: Long
        get() = viewController.rmpMediaTime.value ?: 0

    init {
        repository.appData.distinctUntilChanged().observeForever{ appData ->
            onAppDataUpdated(appData)
        }

        serviceController.serviceGuidUrls.observeForever { urls ->
            onServiceGuidUrls(urls)
        }

        viewController.rmpMediaUrl.distinctUntilChanged().observeForever {
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
        sessions.forEach { socket ->
            socket.sendMessage(message)
        }
    }

    private fun onAppDataUpdated(appData: AppData?) {
        appData?.let {
            if (appData.isAppEquals(currentAppData)) {
                if (subscribedNotifications.contains(NotificationType.SERVICE_CHANGE)) {
                    rpcNotifier.notifyServiceChange(it.appContextId)
                }
            }
        }
        currentAppData = appData
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
            while(isActive) {
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
        private val SUPPORTED_NOTIFICATIONS = setOf(
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