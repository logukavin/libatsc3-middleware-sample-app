package com.nextgenbroadcast.mobile.core.telemetry.aws

data class AWSIoTEvent(
     val topic: String,
     val payload: Any
)