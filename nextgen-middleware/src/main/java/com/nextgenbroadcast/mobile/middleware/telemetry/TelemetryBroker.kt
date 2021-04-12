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
import java.util.concurrent.ConcurrentHashMap

class TelemetryBroker(
        private val readers: List<ITelemetryReader>,
        private val writers: List<ITelemetryWriter>,
        private val controls: List<ITelemetryControl>
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val eventFlow = MutableSharedFlow<TelemetryEvent>(
            replay = 30,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val readerJobs = ConcurrentHashMap<String, Job>()

    private var job: Job? = null

    var testCase: String? = null

    //TODO: add start/stop delay?
    @Synchronized
    fun start() {
        if (job != null) return

        writers.forEach { writer ->
            try {
                writer.open()
            } catch (e: Exception) {
                LOG.e(TAG, "Can't Open writer: ${writer::class.java.simpleName}", e)
            }
        }

        job = coroutineScope.launch {
            try {
                readers.forEach { reader ->
                    launchReader(reader)
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

    private fun CoroutineScope.launchReader(reader: ITelemetryReader) {
        readerJobs[reader.name] = launch {
            reader.read(eventFlow)
        }.apply {
            invokeOnCompletion {
                readerJobs.remove(reader.name, this)
            }
        }
    }

    @Synchronized
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

        readerJobs.values.forEach { job ->
            if (!job.isCancelled) job.cancel()
        }
        readerJobs.clear()

        writers.forEach { writer ->
            try {
                writer.close()
            } catch (e: Exception) {
                LOG.e(TAG, "Can't Close writer: ${writer::class.java.simpleName}", e)
            }
        }

        eventFlow.resetReplayCache()
    }

    fun setReaderEnabled(name: String, enabled: Boolean) {
        if (!isStarted()) return

        if (enabled) {
            readers.forEach { reader ->
                if (reader.name.startsWith(name)) {
                    if (!readerJobs.containsKey(reader.name)) {
                        coroutineScope.launchReader(reader)
                    }
                }
            }
        } else {
            readerJobs.forEach { (readerName, job) ->
                if (readerName.startsWith(name)) {
                    job.cancel()
                }
            }
        }
    }

    fun setReaderDelay(name: String, delayMils: Long) {
        if (!isStarted() || delayMils < 0) return

        readers.forEach { reader ->
            if (reader.name.startsWith(name)) {
                readerJobs[reader.name]?.cancel()
                reader.delayMils = delayMils
                coroutineScope.launchReader(reader)
            }
        }
    }

    private fun isStarted() = job != null

    companion object {
        val TAG: String = TelemetryBroker::class.java.simpleName
    }
}
