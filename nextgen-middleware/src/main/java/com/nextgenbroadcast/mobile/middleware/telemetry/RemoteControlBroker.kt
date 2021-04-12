package com.nextgenbroadcast.mobile.middleware.telemetry

import com.nextgenbroadcast.mobile.middleware.telemetry.control.ITelemetryControl
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class RemoteControlBroker(
        private val controls: List<ITelemetryControl>,
        private val onCommand: suspend (action: String, arguments: Map<String, String>) -> Unit
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // it's MUST be non suspending because we use tryEmit in callback
    private val commandFlow = MutableSharedFlow<TelemetryControl>(
            replay = 2,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var job: Job? = null

    fun start() {
        if (job != null) return

        job = coroutineScope.launch {
            controls.forEach { control ->
                launch {
                    control.subscribe(commandFlow)
                }
            }

            commandFlow.collect { command ->
                onCommand(command.action, command.arguments)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}