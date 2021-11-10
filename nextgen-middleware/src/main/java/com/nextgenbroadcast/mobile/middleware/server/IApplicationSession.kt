package com.nextgenbroadcast.mobile.middleware.server

import com.nextgenbroadcast.mobile.middleware.gateway.IApplicationInterface
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType

interface IApplicationSession : IApplicationInterface {
    enum class Params {
        DeviceId, AdvertisingId, ServiceId, Language, MediaUrl, AppBaseUrl, PlaybackState
    }

    fun notify(type: NotificationType, payload: Any)

    fun getParam(param: Params): String?

    fun subscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType>
    fun unsubscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType>
}