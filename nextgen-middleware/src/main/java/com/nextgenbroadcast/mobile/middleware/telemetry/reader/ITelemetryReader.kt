package com.nextgenbroadcast.mobile.middleware.telemetry.reader

import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryEvent
import kotlinx.coroutines.flow.MutableSharedFlow

interface ITelemetryReader {
    suspend fun read(eventFlow: MutableSharedFlow<TelemetryEvent>)
}