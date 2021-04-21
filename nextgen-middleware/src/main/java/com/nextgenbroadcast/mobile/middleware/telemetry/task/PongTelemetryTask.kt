package com.nextgenbroadcast.mobile.middleware.telemetry.task

import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIotThing
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryEvent
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryPayload
import kotlinx.coroutines.flow.MutableSharedFlow

class PongTelemetryTask : ITelemetryTask {
    override val name = "pongTask"
    override var delayMils = 0L

    override suspend fun read(eventFlow: MutableSharedFlow<TelemetryEvent>) {
        eventFlow.emit(TelemetryEvent(AWSIotThing.AWSIOT_TOPIC_PING, PongData()))
    }
}

data class PongData(
        val name: String = "pong"
): TelemetryPayload()