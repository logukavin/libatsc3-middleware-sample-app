package com.nextgenbroadcast.mobile.middleware.dev.telemetry

import kotlinx.coroutines.flow.MutableSharedFlow

interface ITelemetryObserver {
    suspend fun read(eventFlow: MutableSharedFlow<TelemetryEvent>)
}