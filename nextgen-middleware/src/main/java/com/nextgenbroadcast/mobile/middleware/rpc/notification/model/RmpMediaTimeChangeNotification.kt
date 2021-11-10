package com.nextgenbroadcast.mobile.middleware.rpc.notification.model

import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType

class RmpMediaTimeChangeNotification(
        mediaTimeMills: Double
) : RPCNotification(NotificationType.RMP_MEDIA_TIME_CHANGE) {
        var currentTime: String = String.format("%.3f", mediaTimeMills / 1000)
}