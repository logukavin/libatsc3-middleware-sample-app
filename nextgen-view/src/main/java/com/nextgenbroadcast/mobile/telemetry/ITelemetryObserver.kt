package com.nextgenbroadcast.mobile.telemetry

import kotlinx.coroutines.flow.MutableSharedFlow

interface ITelemetryObserver {
    suspend fun read(eventFlow: MutableSharedFlow<TelemetryEvent>)
}