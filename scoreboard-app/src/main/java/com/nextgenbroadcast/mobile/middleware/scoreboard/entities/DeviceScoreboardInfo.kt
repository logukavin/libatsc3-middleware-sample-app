package com.nextgenbroadcast.mobile.middleware.scoreboard.entities

data class DeviceScoreboardInfo(
    val device: TelemetryDevice,
    val selected: Boolean,
    val distance: Float? = null
)