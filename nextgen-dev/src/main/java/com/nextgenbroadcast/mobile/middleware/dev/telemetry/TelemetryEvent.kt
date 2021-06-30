package com.nextgenbroadcast.mobile.middleware.dev.telemetry

import com.google.gson.JsonElement
import com.google.gson.JsonNull

// such declaration required to support default values with Gson
class TelemetryEvent() {
    var topic: String = ""
        private set
    var payload: JsonElement = JsonNull.INSTANCE
        private set
    val timestamp: Long = System.currentTimeMillis()

    constructor(topic: String, payload: JsonElement) : this() {
        this.topic = topic
        this.payload = payload
    }
}