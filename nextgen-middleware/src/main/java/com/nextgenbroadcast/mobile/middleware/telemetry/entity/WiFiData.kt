package com.nextgenbroadcast.mobile.middleware.telemetry.entity

data class WiFiData(
        val name:String,
        val ip: String,
        val commonInfo: String):TelemetryPayload()