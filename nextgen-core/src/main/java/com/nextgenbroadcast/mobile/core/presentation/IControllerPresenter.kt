package com.nextgenbroadcast.mobile.core.presentation

import kotlinx.coroutines.flow.SharedFlow

interface IControllerPresenter {
    fun setTelemetryEnabled(enabled: Boolean)
    fun setTelemetryEnabled(type: String, enabled: Boolean)
    fun setTelemetryUpdateDelay(type: String, delayMils: Long)
    fun debugInfoSettings(): SharedFlow<Map<String, Boolean>>
}