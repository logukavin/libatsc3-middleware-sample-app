package com.nextgenbroadcast.mobile.middleware.gateway.rpc

import androidx.lifecycle.distinctUntilChanged
import com.nextgenbroadcast.mobile.core.model.AppData
import kotlinx.coroutines.*
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.rpc.notification.RPCNotifier
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.Urls
import com.nextgenbroadcast.mobile.middleware.ws.MiddlewareWebSocket
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RPCGatewayImpl @Inject constructor(
        private val serviceController: IServiceController,
        private val viewController: IViewController,
        mainDispatcher: CoroutineDispatcher,
        ioDispatcher: CoroutineDispatcher
) : IRPCGateway {
    private val mainScope = CoroutineScope(mainDispatcher)
    private val ioScope = CoroutineScope(ioDispatcher)

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
        viewController.appData.distinctUntilChanged().observeForever { appData ->
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
        ioScope.launch {
            sessions.forEach { socket ->
                socket.sendMessage(message)
            }
        }
    }

    private fun onAppDataUpdated(appData: AppData?) {
        //TODO: It's incorrect. Should notify that service changed if we stay on the same BA
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