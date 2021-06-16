package com.nextgenbroadcast.mobile.telemetry

import com.google.gson.JsonElement

class TelemetryEvent(
        val topic: String,
        val payload: JsonElement
)