package com.nextgenbroadcast.mobile.middleware.telemetry.entity

data class WiFiData(
        val name:String,
        val ipv4: String,
        val commonInfo: String):TelemetryPayload()