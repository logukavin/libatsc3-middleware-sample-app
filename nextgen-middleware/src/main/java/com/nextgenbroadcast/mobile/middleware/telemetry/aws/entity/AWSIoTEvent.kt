package com.nextgenbroadcast.mobile.middleware.telemetry.aws.entity

data class AWSIoTEvent(
     val topic: String,
     val payload: AWSIoTPayload
)