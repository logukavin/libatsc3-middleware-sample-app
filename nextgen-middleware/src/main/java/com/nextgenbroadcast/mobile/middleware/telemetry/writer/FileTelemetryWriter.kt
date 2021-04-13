package com.nextgenbroadcast.mobile.middleware.telemetry.writer

import com.google.gson.Gson
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.lang.IllegalStateException

class FileTelemetryWriter(
        private val cacheDir: File,
        private val fileName: String
) : ITelemetryWriter {
    private val gson = Gson()

    private var file: RandomAccessFile? = null

    override fun open() {
        if (file != null) throw IllegalStateException("File already opened")

        file = RandomAccessFile(File(cacheDir, fileName), "rw")
    }

    override fun close() {
        file?.close()
        file = null
    }

    override suspend fun write(eventFlow: Flow<TelemetryEvent>) {
        eventFlow.collect { event ->
            try {
                val line = gson.toJson(event) + "\n"
                file?.write(line.toByteArray(Charsets.US_ASCII))
            } catch (e: IOException) {
                LOG.d(TAG, "Can't store telemetry topic: ${event.topic}", e)
            }
        }
    }

    companion object {
        val TAG: String = FileTelemetryWriter::class.java.simpleName
    }
}