package com.nextgenbroadcast.mobile.middleware.telemetry.control

import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIotThing
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryControl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow

internal class AWSIoTelemetryControl(
        private val thing: AWSIotThing
) : ITelemetryControl {

    override suspend fun subscribe(commandFlow: MutableSharedFlow<TelemetryControl>) {
        supervisorScope {
            while (isActive) {
                try {
                    thing.subscribeCommandsFlow(commandFlow)
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