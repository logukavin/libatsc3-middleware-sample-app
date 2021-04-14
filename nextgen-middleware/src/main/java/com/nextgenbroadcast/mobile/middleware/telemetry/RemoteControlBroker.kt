package com.nextgenbroadcast.mobile.middleware.telemetry

import com.nextgenbroadcast.mobile.middleware.telemetry.control.ITelemetryControl
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryControl
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import java.util.concurrent.ConcurrentHashMap

class RemoteControlBroker(
        _controls: List<ITelemetryControl>,
        private val onCommand: suspend (action: String, arguments: Map<String, String>) -> Unit
) {
    private val controls = mutableListOf(*_controls.toTypedArray())
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // it's MUST be non suspending because we use tryEmit in callback
    private val commandFlow = MutableSharedFlow<TelemetryControl>(
            replay = 2,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val controlJobs = ConcurrentHashMap<ITelemetryControl, Job>()

    private var job: Job? = null

    fun start() {
        if (job != null) return

        job = coroutineScope.launch {
            controls.forEach { control ->
                launchControl(control)
            }

            commandFlow.collect { command ->
                withContext(Dispatchers.Main) {
                    onCommand(command.action, command.arguments)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null

        controlJobs.values.forEach { job ->
            if (!job.isCancelled) job.cancel()
        }
        controlJobs.clear()
    }

    fun addControl(control: ITelemetryControl) {
        if (controls.contains(control) || controlJobs.containsKey(control)) return

        controls.add(control)
        coroutineScope.launchControl(control)
    }

    private fun CoroutineScope.launchControl(control: ITelemetryControl) {
        controlJobs[control] = launch {
            control.subscribe(commandFlow)
        }.apply {
            invokeOnCompletion {
                controlJobs.remove(control, this)
            }
        }
    }
}