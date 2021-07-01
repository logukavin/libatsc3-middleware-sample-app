package com.nextgenbroadcast.mobile.middleware.telemetry.control

import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIoThing
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryControl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow

internal class AWSIoTelemetryControl(
    private val controlTopic: String,
    private val globalTopic: String,
    private val thing: AWSIoThing
) : ITelemetryControl {

    override suspend fun subscribe(commandFlow: MutableSharedFlow<TelemetryControl>) {
        supervisorScope {
            while (isActive) {
                try {
                    launch {
                        thing.subscribe(controlTopic) { _, command ->
                            commandFlow.tryEmit(command)
                        }
                    }

                    launch {
                        thing.subscribe(globalTopic) { topic, command ->
                            command.action = topic.substring(topic.lastIndexOf("/") + 1)
                            commandFlow.tryEmit(command)
                        }
                    }
                } catch (e: Exception) {
                    LOG.d(TAG, "AWS IoT command topic subscription error", e)
                }

                if (isActive) {
                    delay(10_000)
                }
            }
        }
    }

    companion object {
        val TAG: String = AWSIoTelemetryControl::class.java.simpleName
    }
}