package com.nextgenbroadcast.mobile.middleware.telemetry

import android.hardware.Sensor
import android.hardware.SensorManager
import java.util.concurrent.TimeUnit

class LinearAccelerationTelemetry(
        sensorManager: SensorManager,
        sensorDelay: Int = UPDATE_FREQUENCY.toInt()
) : SensorTelemetry(sensorManager, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), sensorDelay) {

    companion object {
        val UPDATE_FREQUENCY = TimeUnit.SECONDS.toMicros(1)
    }
}