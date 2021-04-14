package com.nextgenbroadcast.mobile.middleware.telemetry

import androidx.annotation.MainThread
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryEvent
import com.nextgenbroadcast.mobile.middleware.telemetry.reader.ITelemetryReader
import com.nextgenbroadcast.mobile.middleware.telemetry.writer.ITelemetryWriter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.lang.Exception
import java.util.concurrent.ConcurrentHashMap

class TelemetryBroker(
        private val readers: List<ITelemetryReader>,
        _writers: List<ITelemetryWriter>
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val writers = mutableListOf(*_writers.toTypedArray())
    private val eventFlow = MutableSharedFlow<TelemetryEvent>(
            replay = 30,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val testEventFlow = eventFlow.onEach { event ->
        event.payload.testCase = testCase
    }
    private val readerJobs = ConcurrentHashMap<ITelemetryReader, Job>()
    private val writerJobs = ConcurrentHashMap<ITelemetryWriter, Job>()

    private var isStarted = false

    var testCase: String? = null

    private val _readersEnabled: MutableStateFlow<Map<String, Boolean>> = MutableStateFlow(
            readers.map { it.name to false }.toMap()
    )
    private val _readersDelay: MutableStateFlow<Map<String, Long>> = MutableStateFlow(
            readers.map { it.name to it.delayMils }.toMap()
    )

    val readersEnabled = _readersEnabled.asStateFlow()
    val readersDelay = _readersDelay.asStateFlow()

    //TODO: add start/stop delay?
    @MainThread
    fun start() {
        if (isStarted) return

        _readersEnabled.value = readers.map { it.name to true }.toMap()

        internalStart()
    }

    private fun internalStart() {
        if (isStarted) return

        writers.forEach { writer ->
            try {
                writer.open()
            } catch (e: Exception) {
                LOG.e(TAG, "Can't Open writer: ${writer::class.java.simpleName}", e)
            }
        }

        try {
            val map = _readersEnabled.value

            readers.forEach { reader ->
                if (map[reader.name] == true) {
                    coroutineScope.launchReader(reader)
                }
            }

            writers.forEach { writer ->
                coroutineScope.launchWriter(writer)
            }
        } catch (e: Exception) {
            LOG.d(TAG, "Telemetry gathering error: ", e)
        }

        isStarted = true
    }

    private fun CoroutineScope.launchReader(reader: ITelemetryReader) {
        readerJobs.put(reader,
                launch {
                    reader.read(eventFlow)
                }.apply {
                    invokeOnCompletion {
                        readerJobs.remove(reader, this)
                    }
                }
        )?.let {
            LOG.e(TAG, "Reader is duplicated: ${reader.name}")
            it.cancel()
        }
    }

    private fun CoroutineScope.launchWriter(writer: ITelemetryWriter) {
        writerJobs.put(writer,
                launch {
                    writer.write(testEventFlow)
                }.apply {
                    invokeOnCompletion {
                        writerJobs.remove(writer, this)
                    }
                }
        )?.let {
            LOG.e(TAG, "Writes is duplicated: $writer")
            it.cancel()
        }
    }

    @MainThread
    fun stop() {
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

        writerJobs.values.forEach { job ->
            if (!job.isCancelled) job.cancel()
        }
        writerJobs.clear()

        eventFlow.resetReplayCache()

        // order is important
        isStarted = false
        _readersEnabled.value = readers.map { it.name to false }.toMap()
    }

    @MainThread
    fun addWriter(writer: ITelemetryWriter) {
        if (writers.contains(writer) || writerJobs.containsKey(writer)) return

        writers.add(writer)
        if (isStarted) {
            coroutineScope.launchWriter(writer)
        }
    }

    @MainThread
    fun setReaderEnabled(name: String, enabled: Boolean) {
        val map = _readersEnabled.value.toMutableMap()
        var changed = false

        if (enabled) {
            readers.forEach { reader ->
                if (reader.name.startsWith(name)) {
                    if (!readerJobs.containsKey(reader)) {
                        if (isStarted) {
                            coroutineScope.launchReader(reader)
                        }
                        map[reader.name] = enabled
                        changed = true
                    }
                }
            }
        } else {
            readerJobs.forEach { (reader, job) ->
                if (reader.name.startsWith(name)) {
                    job.cancel()
                    map[reader.name] = enabled
                    changed = true
                }
            }
        }

        if (changed) {
            _readersEnabled.value = map
            if (enabled && !isStarted) {
                internalStart()
            } else if (!enabled && isStarted) {
                // stop broker if all readers switched off
                val dis = map.values.distinct()
                if (dis.size == 1 && !dis.first()) {
                    stop()
                }
            }
        }
    }

    @MainThread
    fun setReaderDelay(name: String, delayMils: Long) {
        if (!isStarted || delayMils < 0) return

        readers.forEach { reader ->
            if (reader.name.startsWith(name)) {
                if (reader.delayMils != delayMils) {
                    readerJobs[reader]?.cancel()
                    reader.delayMils = delayMils
                    coroutineScope.launchReader(reader)
                }
            }
        }
    }

    companion object {
        val TAG: String = TelemetryBroker::class.java.simpleName
    }
}
