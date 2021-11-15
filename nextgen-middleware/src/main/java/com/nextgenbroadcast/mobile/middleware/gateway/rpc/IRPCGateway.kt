package com.nextgenbroadcast.mobile.middleware.gateway.rpc

import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.gateway.IApplicationInterface
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.server.IApplicationSession

interface IRPCGateway : IApplicationInterface {
    val deviceId: String
    val advertisingId: String
    val language: String
    val queryServiceId: String?
    val mediaUrl: String?
    val currentAppBaseUrl: String?
    val playbackState: PlaybackState
    val playbackTime: Long

    fun registerSession(session: IApplicationSession)
    fun unregisterSession(session: IApplicationSession)

    fun filterAvailableNotifications(requested: Set<NotificationType>): Set<NotificationType>
}