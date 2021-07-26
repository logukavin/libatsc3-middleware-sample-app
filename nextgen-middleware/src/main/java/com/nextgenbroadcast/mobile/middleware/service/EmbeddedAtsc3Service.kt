package com.nextgenbroadcast.mobile.middleware.service

import android.os.Binder
import android.os.IBinder
import com.nextgenbroadcast.mobile.core.asReadOnly
import com.nextgenbroadcast.mobile.middleware.dev.presentation.*
import com.nextgenbroadcast.mobile.middleware.Atsc3ReceiverCore
import com.nextgenbroadcast.mobile.middleware.MiddlewareConfig
import com.nextgenbroadcast.mobile.middleware.dev.service.binder.IServiceBinder
import kotlinx.coroutines.flow.*

class EmbeddedAtsc3Service : Atsc3ForegroundService() {

    override fun createServiceBinder(receiver: Atsc3ReceiverCore): IBinder =
        object : Binder(), IServiceBinder {
            override val controllerPresenter = if (MiddlewareConfig.DEV_TOOLS) {
                object : IControllerPresenter {
                    //TODO: isolate Flow to prevent internal objects blocking with UI
                    // maybe something like this: .stateIn(viewController.scope(), SharingStarted.Lazily, telemetryBroker.readersEnabled.value)
                    override val telemetryEnabled = telemetryHolder.telemetryEnabled.asReadOnly()
                    override val telemetryDelay = telemetryHolder.telemetryDelay.asReadOnly()
                    override val debugInfoSettings = telemetryHolder.debugInfoSettings.asReadOnly()

                    override fun setDebugInfoVisible(type: String, visible: Boolean) {
                        telemetryHolder.setInfoVisible(visible, type)
                    }

                    override fun setTelemetryEnabled(enabled: Boolean) {
                        telemetryHolder.setTelemetryEnabled(enabled)
                    }

                    override fun setTelemetryEnabled(type: String, enabled: Boolean) {
                        telemetryHolder.setTelemetryEnabled(enabled, type)
                    }

                    override fun setTelemetryUpdateDelay(type: String, delayMils: Long) {
                        telemetryHolder.setTelemetryDelay(delayMils, type)
                    }
                }
            } else {
                object : IControllerPresenter {
                    override val telemetryEnabled = MutableStateFlow<Map<String, Boolean>>(emptyMap()).asReadOnly()
                    override val telemetryDelay = MutableStateFlow<Map<String, Long>>(emptyMap()).asReadOnly()
                    override val debugInfoSettings = MutableStateFlow<Map<String, Boolean>>(emptyMap()).asReadOnly()

                    override fun setDebugInfoVisible(type: String, visible: Boolean) {
                    }

                    override fun setTelemetryEnabled(enabled: Boolean) {
                    }

                    override fun setTelemetryEnabled(type: String, enabled: Boolean) {
                    }

                    override fun setTelemetryUpdateDelay(type: String, delayMils: Long) {
                    }
                }
            }
        }

    companion object {
        const val SERVICE_INTERFACE = Atsc3ForegroundService.SERVICE_INTERFACE

        fun init() {
            clazz = EmbeddedAtsc3Service::class.java
        }
    }
}