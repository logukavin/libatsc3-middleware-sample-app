package com.nextgenbroadcast.mobile.middleware.telemetry

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIotThing
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.entity.AWSIoTEvent
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.entity.AWSIoTPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

class SensorTelemetry(
        private val sensorManager: SensorManager,
        private val sensorType: Int,
        private val sensorDelay: Long = DEFAULT_UPDATE_FREQUENCY
) {
    private val emptyFloatArray = FloatArray(0)
    private val sensor: Sensor? = sensorManager.getDefaultSensor(sensorType)

    suspend fun start(eventFlow: MutableSharedFlow<AWSIoTEvent>) {
        if (sensor == null) return

        callbackFlow<SensorData> {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    safeSend(SensorData(
                            sensorName = event.sensor.name,
                            values = event.values,
                            accuracy = event.accuracy
                    ))
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                    safeSend(SensorData(
                            sensorName = sensor.name,
                            values = emptyFloatArray,
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

            sensorManager.registerListener(listener, sensor, (sensorDelay * 1000).toInt())

            awaitClose {
                sensorManager.unregisterListener(listener)
            }
        }.buffer(Channel.CONFLATED) // To avoid send blocking
                .sample(sensorDelay) // To control emission frequency
                .collect { data ->
                    eventFlow.emit(AWSIoTEvent(AWSIotThing.AWSIOT_TOPIC_SENSORS, data))
                }
    }

    companion object {
        val TAG: String = SensorTelemetry::class.java.simpleName

        val DEFAULT_UPDATE_FREQUENCY = TimeUnit.SECONDS.toMillis(1)
    }
}

data class SensorData(
        val sensorName: String,
        val values: FloatArray,
        val accuracy: Int
) : AWSIoTPayload() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorData

        if (sensorName != other.sensorName) return false
        if (accuracy != other.accuracy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sensorName.hashCode()
        result = 31 * result + accuracy
        return result
    }
}