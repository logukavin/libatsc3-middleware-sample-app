package com.nextgenbroadcast.mobile.middleware.rpc.notification.model

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.rpc.processor.serializer.DoubleWithPrecisionThreeJsonSerializer

// such time declaration is used to add custom serialization
class RmpMediaTimeChangeNotification(
    mediaTime: Double
) : RPCNotification(NotificationType.RMP_MEDIA_TIME_CHANGE) {
    @JsonSerialize(using = DoubleWithPrecisionThreeJsonSerializer::class)
    val currentTime: Double = mediaTime
}