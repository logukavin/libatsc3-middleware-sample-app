package com.nextgenbroadcast.mobile.middleware.telemetry.control

import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIotThing
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryControl
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect

class AWSIoTelemetryControl(
        private val thing: AWSIotThing,
        private val onCommand: suspend (action: String, arguments: Map<String, String>) -> Unit
) : ITelemetryControl {
    // it's MUST be non suspending because we use tryEmit in callback
    private val commandFlow = MutableSharedFlow<TelemetryControl>(replay = 1, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var job: Job? = null

    override fun subscribe() {
        job = CoroutineScope(Dispatchers.IO).launch {
            // Command processing
            launch {
                thing.subscribeCommandsFlow(commandFlow)
            }
            launch {
                commandFlow.collect { command ->
                    onCommand(command.action, command.arguments)
                }
            }
        }
    }

    override fun unsubscribe() {
        job?.cancel()
        job = null
    }
}