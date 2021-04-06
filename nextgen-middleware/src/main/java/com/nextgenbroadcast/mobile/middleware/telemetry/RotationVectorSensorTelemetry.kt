package com.nextgenbroadcast.mobile.middleware.telemetry

import android.hardware.Sensor
import android.hardware.SensorManager

class RotationVectorSensorTelemetry(
        sensorManager: SensorManager,
        sensorDelay: Int = DEFAULT_UPDATE_FREQUENCY.toInt()
) : SensorTelemetry(sensorManager, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), sensorDelay)