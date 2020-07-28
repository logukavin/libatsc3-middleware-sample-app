package org.ngbp.jsonrpc4jtestharness.gateway.rpc

import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Urls

interface IRPCGateway {
    val language: String
    val queryServiceId: String?
    val mediaUrl: String?
    val playbackState: PlaybackState
    val serviceGuideUrls: List<Urls>

    fun updateRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double)

    fun requestMediaPlay(mediaUrl: String? = null, delay: Long)
    fun requestMediaStop(delay: Long)

    fun subscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType>
    fun unsubscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType>

    fun sendNotification(message: String)
}