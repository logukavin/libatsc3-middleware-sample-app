package com.nextgenbroadcast.mobile.middleware.gateway.rpc

import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.Urls
import com.nextgenbroadcast.mobile.middleware.ws.MiddlewareWebSocket

interface IRPCGateway {
    val language: String
    val queryServiceId: String?
    val mediaUrl: String?
    val playbackState: PlaybackState
    val serviceGuideUrls: List<Urls>

    fun onSocketOpened(socket: MiddlewareWebSocket)
    fun onSocketClosed(socket: MiddlewareWebSocket)

    fun updateRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double)

    fun requestMediaPlay(mediaUrl: String? = null, delay: Long)
    fun requestMediaStop(delay: Long)

    fun subscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType>
    fun unsubscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType>

    fun sendNotification(message: String)
}