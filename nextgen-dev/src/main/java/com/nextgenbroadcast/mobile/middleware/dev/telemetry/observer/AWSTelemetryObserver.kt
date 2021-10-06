package com.nextgenbroadcast.mobile.middleware.dev.telemetry.observer

import com.google.gson.JsonPrimitive
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.aws.AWSIoThing
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.ClientTelemetryEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class AWSTelemetryObserver(
    eventTopicFormat: String,
    private val awsIoThing: AWSIoThing,
    private val clientId: String,
    private val topics: List<String>
) : ITelemetryObserver {
    private val eventTopic = eventTopicFormat.replace(AWSIoThing.AWSIOT_FORMAT_SERIAL, clientId)

    override suspend fun read(eventFlow: MutableSharedFlow<ClientTelemetryEvent>) {
        supervisorScope {
            topics.forEach { nextTopic ->
                launch {
                    try {
                        awsIoThing.subscribe("$eventTopic/$nextTopic") { topic, payload ->
                            if (payload != null) {
                                eventFlow.tryEmit(ClientTelemetryEvent(
                                    topic,
                                    JsonPrimitive(payload)
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        LOG.d(TAG, "Failed observing topic $nextTopic on device: $clientId", e)
                    }
                }
            }
        }
    }

    companion object {
        val TAG: String = AWSTelemetryObserver::class.java.simpleName
    }
}