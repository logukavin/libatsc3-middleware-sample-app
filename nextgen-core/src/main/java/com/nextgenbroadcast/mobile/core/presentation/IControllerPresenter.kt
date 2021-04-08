package com.nextgenbroadcast.mobile.core.presentation

interface IControllerPresenter {
    fun setTelemetryEnabled(enabled: Boolean)
    fun setTelemetryEnabled(type: String, enabled: Boolean)
    fun setTelemetryUpdateDelay(type: String, delayMils: Long)
}