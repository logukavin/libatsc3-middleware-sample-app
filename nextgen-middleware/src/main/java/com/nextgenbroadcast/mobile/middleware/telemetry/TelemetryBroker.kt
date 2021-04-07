package com.nextgenbroadcast.mobile.middleware.telemetry

import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.telemetry.control.ITelemetryControl
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryEvent
import com.nextgenbroadcast.mobile.middleware.telemetry.reader.ITelemetryReader
import com.nextgenbroadcast.mobile.middleware.telemetry.writer.ITelemetryWriter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import java.lang.Exception

class TelemetryBroker(
        private val readers: List<ITelemetryReader>,
        private val writers: List<ITelemetryWriter>,
        private val controls: List<ITelemetryControl>
) {
    private val eventFlow = MutableSharedFlow<TelemetryEvent>(
            replay = 30,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

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
                readers.forEach { reader ->
                    launch {
                        reader.read(eventFlow)
                    }
                }

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
