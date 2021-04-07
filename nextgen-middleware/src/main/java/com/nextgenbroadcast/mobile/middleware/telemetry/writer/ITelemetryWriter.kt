package com.nextgenbroadcast.mobile.middleware.telemetry.writer

import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryEvent

interface ITelemetryWriter {
    fun open()
    fun close()
    fun write(event: TelemetryEvent)
}