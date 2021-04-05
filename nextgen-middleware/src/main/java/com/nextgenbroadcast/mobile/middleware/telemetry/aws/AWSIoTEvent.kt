package com.nextgenbroadcast.mobile.middleware.telemetry.aws

data class AWSIoTEvent(
     val topic: String,
     val payload: Any
)