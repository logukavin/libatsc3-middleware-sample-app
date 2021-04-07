package com.nextgenbroadcast.mobile.middleware.telemetry.writer

import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIotThing
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryEvent

class AWSIoTelemetryWriter(
        private val thing: AWSIotThing
) : ITelemetryWriter {

    override fun open() {
        thing.connect()
    }

    override fun close() {
        thing.disconnect()
    }

    override fun write(event: TelemetryEvent) {
        thing.publish(event.topic, event.payload)
    }
}