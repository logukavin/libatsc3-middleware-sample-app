package com.nextgenbroadcast.mobile.middleware.dev.telemetry.writer

import com.amazonaws.services.iot.client.core.AwsIotRuntimeException
import com.google.gson.Gson
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.aws.AWSIoThing
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.TelemetryEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

class AWSIoTelemetryWriter(
        private val thing: AWSIoThing
) : ITelemetryWriter {
    private val gson = Gson()

    override fun open() {
    }

    override fun close() {
    }

    override suspend fun write(eventFlow: Flow<TelemetryEvent>) {
        eventFlow.collect { event ->
            try {
                LOG.d(TAG, "AWS IoT event: ${event.topic} - ${event.payload}")
                thing.publish(event.topic, gson.toJson(event.payload))
            } catch (e: AwsIotRuntimeException) {
                LOG.e(TAG, "Can't publish telemetry topic: ${event.topic}", e)
            }
        }
    }

    companion object {
        val TAG: String = AWSIoTelemetryWriter::class.java.simpleName
    }
}