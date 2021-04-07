package com.nextgenbroadcast.mobile.middleware.telemetry.entity

data class TelemetryEvent(
     val topic: String,
     val payload: TelemetryPayload
)