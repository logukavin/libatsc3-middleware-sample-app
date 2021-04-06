package com.nextgenbroadcast.mobile.middleware.telemetry

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIoTEvent
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIotThing
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect

open class SensorTelemetry(
        private val sensorManager: SensorManager,
        private val sensor: Sensor?,
        private val sensorDelay: Int
) {
    suspend fun start(eventFlow: MutableSharedFlow<AWSIoTEvent>) {
        if (sensor == null) return

        callbackFlow<SensorData> {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    safeSend(SensorData(
                            sensorName = event.sensor.name,
                            accuracy = 0
                    ))
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                    safeSend(SensorData(
                            sensorName = sensor.name,
                            accuracy = accuracy
                    ))
                }

                private fun safeSend(data: SensorData) {
                    try {
                        sendBlocking(data)
                    } catch (e: Exception) {
                        LOG.e(TAG, "Error on sending ${sensor.name} sensor data", e)
                    }
                }
            }

            sensorManager.registerListener(listener, sensor, sensorDelay)

            awaitClose {
                sensorManager.unregisterListener(listener)
            }
        }.apply {
            buffer(Channel.CONFLATED) // To avoid blocking
        }.collect { data ->
            eventFlow.emit(AWSIoTEvent(AWSIotThing.AWSIOT_TOPIC_SENSORS, data))
        }
    }

    companion object {
        val TAG: String = GyroscopeSensorTelemetry::class.java.simpleName
        val DEFAULT_UPDATE_FREQUENCY = java.util.concurrent.TimeUnit.MINUTES.toMicros(1)
    }
}

data class SensorData(
        val sensorName: String,
        val accuracy: Int
)