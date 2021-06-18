package com.nextgenbroadcast.mobile.middleware.telemetry.entity

abstract class TelemetryPayload {
    var testCase: String? = null
    val timeStamp = System.currentTimeMillis()
}