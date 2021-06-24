package com.nextgenbroadcast.mobile.middleware.dev.telemetry.observer

import com.nextgenbroadcast.mobile.middleware.dev.telemetry.TelemetryEvent
import kotlinx.coroutines.flow.MutableSharedFlow

interface ITelemetryObserver {
    suspend fun read(eventFlow: MutableSharedFlow<TelemetryEvent>)
}