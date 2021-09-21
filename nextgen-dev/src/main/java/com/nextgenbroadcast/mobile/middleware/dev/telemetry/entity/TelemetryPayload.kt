package com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity

abstract class TelemetryPayload {
    val timeStamp = System.currentTimeMillis()
    var testCase: String? = null
    var error: String? = null
}