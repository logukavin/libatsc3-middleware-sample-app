package com.nextgenbroadcast.mobile.middleware.scoreboard.entities

data class TelemetryDevice(
    val id: String,
    val host: String,
    val port: Int,
    val isLost: Boolean = false
)