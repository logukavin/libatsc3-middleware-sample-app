package com.nextgenbroadcast.mobile.middleware.telemetry.control

import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryControl
import kotlinx.coroutines.flow.MutableSharedFlow

interface ITelemetryControl {
    suspend fun subscribe(commandFlow: MutableSharedFlow<TelemetryControl>)
}