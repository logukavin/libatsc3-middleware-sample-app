package com.nextgenbroadcast.mobile.middleware.telemetry.writer

import com.amazonaws.services.iot.client.core.AwsIotRuntimeException
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIotThing
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryEvent
import java.lang.Exception

class AWSIoTelemetryWriter(
        private val thing: AWSIotThing
) : ITelemetryWriter {

    override fun open() {
        try {
            thing.connect()
        } catch (e: Exception) {
            LOG.e(TAG, "Error connecting to AWS IoT", e)
        }
    }

    override fun close() {
        try {
            thing.disconnect()
        } catch (e: Exception) {
            LOG.e(TAG, "Error disconnecting AWS IoT", e)
        }
    }

    override fun write(event: TelemetryEvent) {
        try {
            thing.publish(event.topic, event.payload)
        } catch (e: AwsIotRuntimeException) {
            LOG.e(TAG, "Can't publish telemetry topic: ${event.topic}", e)
        }
    }

    companion object {
        val TAG: String = AWSIoTelemetryWriter::class.java.simpleName
    }
}