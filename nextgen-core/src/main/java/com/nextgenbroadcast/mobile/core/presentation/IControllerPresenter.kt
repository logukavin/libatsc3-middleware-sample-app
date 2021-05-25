package com.nextgenbroadcast.mobile.core.presentation

import kotlinx.coroutines.flow.StateFlow

interface IControllerPresenter {
    val telemetryEnabled: StateFlow<Map<String, Boolean>>
    val telemetryDelay: StateFlow<Map<String, Long>>
    val debugInfoSettings: StateFlow<Map<String, Boolean>>

    fun setDebugInfoVisible(type: String, visible: Boolean)

    fun setTelemetryEnabled(enabled: Boolean)
    fun setTelemetryEnabled(type: String, enabled: Boolean)
    fun setTelemetryUpdateDelay(type: String, delayMils: Long)
}