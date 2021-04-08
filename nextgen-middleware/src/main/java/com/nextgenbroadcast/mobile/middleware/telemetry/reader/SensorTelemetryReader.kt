package com.nextgenbroadcast.mobile.middleware.telemetry.reader

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIotThing
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryEvent
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

class SensorTelemetryReader(
        private val sensorManager: SensorManager,
        private val sensorType: Int,
        private val sensorDelay: Long = DEFAULT_UPDATE_FREQUENCY
) : ITelemetryReader {
    private val emptyFloatArray = FloatArray(0)

    override suspend fun read(eventFlow: MutableSharedFlow<TelemetryEvent>) {
        val sensor: Sensor = sensorManager.getDefaultSensor(sensorType) ?: return

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
                    eventFlow.emit(TelemetryEvent(AWSIotThing.AWSIOT_TOPIC_SENSORS, data))
                }
    }

    companion object {
        val TAG: String = SensorTelemetryReader::class.java.simpleName

        val DEFAULT_UPDATE_FREQUENCY = TimeUnit.SECONDS.toMillis(1)
    }
}

enum class SensorFrequencyType{
    FASTEST, HIGH, MEDIUM, LOW
}

data class SensorData(
        val sensorName: String,
        val values: FloatArray,
        val accuracy: Int
) : TelemetryPayload() {
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