package com.nextgenbroadcast.mobile.middleware.dev.telemetry

import com.google.gson.JsonElement

class TelemetryEvent(
        val topic: String,
        val payload: JsonElement
)