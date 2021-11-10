package com.nextgenbroadcast.mobile.middleware.server

import com.github.nmuzhichin.jsonrpc.model.request.Notification
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.rpc.notification.model.*
import com.nextgenbroadcast.mobile.middleware.rpc.processor.RPCObjectMapper
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.AlertingSignalingRpcResponse
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.ServiceGuideUrlsRpcResponse
import com.nextgenbroadcast.mobile.middleware.server.ws.MiddlewareWebSocket

class MiddlewareApplicationSession(
    rpcGateway: IRPCGateway
): AbstractApplicationSession(rpcGateway) {

    private val rpcObjectMapper = RPCObjectMapper()

    @Volatile
    private var socket: MiddlewareWebSocket? = null

    override val isActive: Boolean
        get() = socket != null

    fun startSession(connection: MiddlewareWebSocket) {
        socket = connection
        super.startSession()
    }

    public override fun finishSession() {
        super.finishSession()
        socket = null
    }

    override fun notify(type: NotificationType, payload: Any) {
        if (isSubscribedNotification(type)) {
            val notification = newNotificationOrNull(type, payload)
            if (notification != null) {
                socket?.sendNotification(notification)

                // Start/Stop RMP playback ticker
                if (type == NotificationType.RMP_PLAYBACK_STATE_CHANGE) {
                    val ntf = notification as? RmpPlaybackStateChangeNotification
                    if (ntf?.playbackState == PlaybackState.PLAYING) {
                        startMediaTimeUpdateJob()
                    } else {
                        cancelMediaTimeUpdateJob()
                    }
                }
            }
        }
    }

    private fun MiddlewareWebSocket.sendNotification(rpcNotification: RPCNotification) {
        val params: Map<String, Any> = rpcObjectMapper.objectToMap(rpcNotification)
        val notification = Notification(NOTIFICATION_METHOD_NAME, params)
        sendMessage(rpcObjectMapper.objectToJson(notification))
    }

    @Suppress("UNCHECKED_CAST")
    private fun newNotificationOrNull(type: NotificationType, payload: Any) =
        try {
            when (type) {
                NotificationType.SERVICE_CHANGE -> ServiceChangeNotification(payload as String)
                NotificationType.CONTENT_CHANGE -> ContentChangeNotification(payload as List<String>)
                NotificationType.SERVICE_GUIDE_CHANGE -> ServiceGuideChangeNotification(
                    (payload as List<Pair<String, String>>).map { (sgType, sgUrl) ->
                        ServiceGuideUrlsRpcResponse.Url(sgType, sgUrl, null, null)
                    }
                )
                NotificationType.RMP_PLAYBACK_STATE_CHANGE -> RmpPlaybackStateChangeNotification(payload as PlaybackState)
                NotificationType.RMP_PLAYBACK_RATE_CHANGE -> RmpPlaybackRateChangeNotification(payload as Float)
                NotificationType.RMP_MEDIA_TIME_CHANGE -> RmpMediaTimeChangeNotification(payload as Double)
                NotificationType.ALERT_CHANGE -> AlertingChangeNotification(
                    listOf(AlertingSignalingRpcResponse.Alert(AlertingSignalingRpcResponse.Alert.AEAT,
                        (payload as List<String>).joinToString(separator = "", prefix = "<AEAT>", postfix = "</AEAT>") { it }
                    ))
                )
                else -> null
            }
        } catch (e: ClassCastException) {
            null
        }

    companion object {
        private const val NOTIFICATION_METHOD_NAME = "org.atsc.notify"
    }
}