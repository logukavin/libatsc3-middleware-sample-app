package com.nextgenbroadcast.mobile.core.presentation

import kotlinx.coroutines.flow.SharedFlow

interface IControllerPresenter {
    val debugInfoSettings: SharedFlow<Map<String, Boolean>>
    fun setTelemetryEnabled(enabled: Boolean)
    fun setTelemetryEnabled(type: String, enabled: Boolean)
    fun setTelemetryUpdateDelay(type: String, delayMils: Long)
}