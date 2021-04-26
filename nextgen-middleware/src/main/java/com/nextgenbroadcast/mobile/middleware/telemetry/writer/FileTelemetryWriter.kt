package com.nextgenbroadcast.mobile.middleware.telemetry.writer

import com.google.gson.Gson
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlin.text.StringBuilder

class FileTelemetryWriter(
        private val dir: File,
        private val fileName: String
) : ITelemetryWriter {
    private val gson = Gson()
    private val fileNameStringBuilder: StringBuilder by lazy {
        StringBuilder(fileName)
    }
    private var randomAccessFile: RandomAccessFile? = null

    override fun open() {
        if (randomAccessFile != null) throw IllegalStateException("File already opened")

        randomAccessFile = RandomAccessFile(getUniqueFile(), "rw")
    }

    private fun getUniqueFile(): File {
        var i = 1
        var file = File(dir, fileName)
        val dotIndex = fileName.lastIndexOf('.')
        while (file.exists()) {
            fileNameStringBuilder.clear()
            if (dotIndex > 0) {
                fileNameStringBuilder.insert(dotIndex, "($i)")
            } else {
                fileNameStringBuilder.append("($i)")
            }
            file = File(dir, fileNameStringBuilder.toString())
            i++
        }
        return file
    }

    override fun close() {
        randomAccessFile?.close()
        randomAccessFile = null
    }

    override suspend fun write(eventFlow: Flow<TelemetryEvent>) {
        eventFlow.collect { event ->
            try {
                val line = gson.toJson(event) + "\n"
                randomAccessFile?.write(line.toByteArray(Charsets.US_ASCII))
            } catch (e: IOException) {
                LOG.d(TAG, "Can't store telemetry topic: ${event.topic}", e)
            }
        }
    }

    companion object {
        val TAG: String = FileTelemetryWriter::class.java.simpleName
    }
}