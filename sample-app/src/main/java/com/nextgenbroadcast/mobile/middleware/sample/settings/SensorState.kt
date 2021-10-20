package com.nextgenbroadcast.mobile.middleware.sample.settings

data class SensorState(
    val sensorName: String,
    var sensorEnable: Boolean,
    var sensorDelay: Long?)