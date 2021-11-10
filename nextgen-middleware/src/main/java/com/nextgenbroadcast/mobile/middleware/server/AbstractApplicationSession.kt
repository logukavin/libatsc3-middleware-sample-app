package com.nextgenbroadcast.mobile.middleware.server

import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.atsc3.ISignalingData
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import kotlinx.coroutines.*

abstract class AbstractApplicationSession(
    private val rpcGateway: IRPCGateway
) : IApplicationSession {
    private val subscribedNotifications: MutableSet<NotificationType> = mutableSetOf()

    private var mediaTimeUpdateJob: Job? = null

    abstract val isActive: Boolean

    protected open fun startSession() {
        rpcGateway.registerSession(this)
    }

    protected open fun finishSession() {
        rpcGateway.unregisterSession(this)
        cancelMediaTimeUpdateJob()
    }

    override fun getParam(param: IApplicationSession.Params): String? {
        return when (param) {
            IApplicationSession.Params.DeviceId -> rpcGateway.deviceId
            IApplicationSession.Params.AdvertisingId -> rpcGateway.advertisingId
            IApplicationSession.Params.ServiceId -> rpcGateway.queryServiceId
            IApplicationSession.Params.Language -> rpcGateway.language
            IApplicationSession.Params.MediaUrl -> rpcGateway.mediaUrl
            IApplicationSession.Params.AppBaseUrl -> rpcGateway.currentAppBaseUrl
            IApplicationSession.Params.PlaybackState -> rpcGateway.playbackState.state.toString()
        }
    }

    override fun requestRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double) {
        if (isActive) {
            rpcGateway.requestRMPPosition(scaleFactor, xPos, yPos)
        }
    }

    override fun requestMediaPlay(mediaUrl: String?, delay: Long) {
        if (isActive) {
            rpcGateway.requestMediaPlay(mediaUrl, delay)
        }
    }

    override fun requestMediaStop(delay: Long) {
        if (isActive) {
            rpcGateway.requestMediaStop(delay)
        }
    }

    override fun subscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType> {
        val available = rpcGateway.filterAvailableNotifications(notifications)
        subscribedNotifications.addAll(available)

        if (available.contains(NotificationType.RMP_MEDIA_TIME_CHANGE)) {
            if (rpcGateway.playbackState == PlaybackState.PLAYING) {
                startMediaTimeUpdateJob()
            }
        }

        return available
    }

    override fun unsubscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType> {
        val available = rpcGateway.filterAvailableNotifications(notifications)
        subscribedNotifications.removeAll(available)

        if (available.contains(NotificationType.RMP_MEDIA_TIME_CHANGE)) {
            cancelMediaTimeUpdateJob()
        }

        return available
    }

    override fun requestFileCache(baseUrl: String?, rootPath: String?, paths: List<String>, filters: List<String>?): Boolean {
        return rpcGateway.requestFileCache(baseUrl, rootPath, paths, filters)
    }

    override fun getServiceGuideUrls(service: String?): List<SGUrl> {
        return rpcGateway.getServiceGuideUrls(service)
    }

    override fun requestServiceChange(globalServiceId: String): Boolean {
        return rpcGateway.requestServiceChange(globalServiceId)
    }

    override fun getAEATChangingList(): List<String> {
        return rpcGateway.getAEATChangingList()
    }

    override fun getSignalingInfo(names: List<String>): List<ISignalingData> {
        return rpcGateway.getSignalingInfo(names)
    }

    protected fun isSubscribedNotification(type: NotificationType): Boolean {
        return subscribedNotifications.contains(type)
    }

    protected fun startMediaTimeUpdateJob() {
        cancelMediaTimeUpdateJob()

        if (!isSubscribedNotification(NotificationType.RMP_MEDIA_TIME_CHANGE)) return

        mediaTimeUpdateJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                onMediaTimeChanged(rpcGateway.playbackTime)

                delay(MEDIA_TIME_UPDATE_DELAY)
            }
            mediaTimeUpdateJob = null
        }
    }

    protected fun cancelMediaTimeUpdateJob() {
        mediaTimeUpdateJob?.let {
            it.cancel()
            mediaTimeUpdateJob = null
        }
    }

    private fun onMediaTimeChanged(mediaTime: Long) {
        if (mediaTime <= 0) return
        if (isSubscribedNotification(NotificationType.RMP_MEDIA_TIME_CHANGE)) {
            notify(NotificationType.RMP_MEDIA_TIME_CHANGE, mediaTime.toDouble())
        }
    }

    companion object {
        private const val MEDIA_TIME_UPDATE_DELAY = 500L
    }
}