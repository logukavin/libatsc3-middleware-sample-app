package com.nextgenbroadcast.mobile.middleware.telemetry.entity

abstract class TelemetryPayload(
        var testCase: String? = null
) {
    private val timeStamp = System.currentTimeMillis()
}