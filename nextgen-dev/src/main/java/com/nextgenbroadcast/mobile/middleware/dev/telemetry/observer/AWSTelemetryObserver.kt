package com.nextgenbroadcast.mobile.middleware.dev.telemetry.observer

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.aws.AWSIoThing
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.ClientTelemetryEvent
import kotlinx.coroutines.flow.MutableSharedFlow

class AWSTelemetryObserver(
    eventTopicFormat: String,
    private val awsIoThing: AWSIoThing,
    private val clientId: String,
    private val topic: String
): ITelemetryObserver {
    private val eventTopic = eventTopicFormat.replace(AWSIoThing.AWSIOT_FORMAT_SERIAL, clientId)
    private val gson = Gson()

    override suspend fun read(eventFlow: MutableSharedFlow<ClientTelemetryEvent>) {
        try {
            awsIoThing.subscribe("$eventTopic/$topic") { topic, event ->
                eventFlow.tryEmit(ClientTelemetryEvent(
                    topic,
                    gson.fromJson(event, JsonObject::class.java)
                ))
            }
        } catch (e: Exception) {
            LOG.d(TAG, "Failed observing topic $topic on device: $clientId", e)
        }
    }

    companion object {
        val TAG: String = AWSTelemetryObserver::class.java.simpleName
    }
}