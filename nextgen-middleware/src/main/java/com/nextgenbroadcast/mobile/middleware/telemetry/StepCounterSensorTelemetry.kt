package com.nextgenbroadcast.mobile.middleware.telemetry

import android.hardware.Sensor
import android.hardware.SensorManager

class StepCounterSensorTelemetry(
        sensorManager: SensorManager,
        sensorDelay: Int = DEFAULT_UPDATE_FREQUENCY.toInt()
) : SensorTelemetry(sensorManager, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER), sensorDelay)