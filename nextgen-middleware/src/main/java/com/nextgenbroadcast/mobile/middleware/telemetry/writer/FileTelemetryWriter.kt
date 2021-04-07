package com.nextgenbroadcast.mobile.middleware.telemetry.writer

import com.google.gson.Gson
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryEvent
import java.io.File
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

    override fun write(event: TelemetryEvent) {
        val line = gson.toJson(event) + "\n"
        file?.write(line.toByteArray(Charsets.US_ASCII))
    }
}