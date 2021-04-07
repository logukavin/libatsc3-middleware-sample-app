package com.nextgenbroadcast.mobile.middleware.telemetry

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.telemetry.control.ITelemetryControl
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryEvent
import com.nextgenbroadcast.mobile.middleware.telemetry.writer.ITelemetryWriter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import java.lang.Exception

class TelemetryBroker(
        context: Context,
        private val writers: List<ITelemetryWriter>,
        private val controls: List<ITelemetryControl>
) {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val eventFlow = MutableSharedFlow<TelemetryEvent>(replay = 20, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var job: Job? = null

    var testCase: String? = null

    fun start() {
        writers.forEach { writer ->
            try {
                writer.open()
            } catch (e: Exception) {
                LOG.e(TAG, "Can't Open writer: ${writer::class.java.simpleName}", e)
            }
        }

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Battery
                launch {
                    BatteryTelemetry(appContext).start(eventFlow)
                }

                // Sensors
                listOf(
                        Sensor.TYPE_LINEAR_ACCELERATION,
                        Sensor.TYPE_GYROSCOPE,
                        Sensor.TYPE_SIGNIFICANT_MOTION,
                        Sensor.TYPE_STEP_DETECTOR,
                        Sensor.TYPE_STEP_COUNTER,
                        Sensor.TYPE_ROTATION_VECTOR
                ).forEach { sensorType ->
                    launch {
                        SensorTelemetry(sensorManager, sensorType).start(eventFlow)
                    }
                }

                // Location
                launch {
                    GPSTelemetry(appContext, GPSTelemetry.Companion.FrequencyType.MEDIUM).start(eventFlow)
                }

                // Telemetry sending
                observeEventFlow()
            } catch (e: Exception) {
                LOG.d(TAG, "Telemetry gathering error: ", e)
            }
        }

        controls.forEach { control ->
            try {
                control.subscribe()
            } catch (e: Exception) {
                LOG.e(TAG, "Can't subscribe control: ${control::class.java.simpleName}", e)
            }
        }
    }

    private suspend fun observeEventFlow() {
        eventFlow.collect { event ->
            LOG.d(TAG, "AWS IoT event: ${event.topic} - ${event.payload}")

            event.payload.testCase = testCase

            writers.forEach { writer ->
                try {
                    writer.write(event)
                } catch (e: Exception) {
                    LOG.e(TAG, "Can't Write to: ${writer::class.java.simpleName}", e)
                }
            }
        }
    }

    fun stop() {
        controls.forEach { control ->
            try {
                control.unsubscribe()
            } catch (e: Exception) {
                LOG.e(TAG, "Can't unsubscribe control: ${control::class.java.simpleName}", e)
            }
        }

        job?.cancel()
        job = null

        writers.forEach { writer ->
            try {
                writer.close()
            } catch (e: Exception) {
                LOG.e(TAG, "Can't Close writer: ${writer::class.java.simpleName}", e)
            }
        }
    }

    companion object {
        val TAG: String = TelemetryBroker::class.java.simpleName
    }
}
