package com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization.model

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse
import com.nextgenbroadcast.mobile.middleware.rpc.processor.serializer.DoubleWithPrecisionThreeJsonSerializer

// such time declaration is used to add custom serialization
class RmpMediaTime(
    mediaTime: Double,
    val startDate: String? = null
) : RpcResponse() {
    @JsonSerialize(using = DoubleWithPrecisionThreeJsonSerializer::class)
    val currentTime: Double = mediaTime
}