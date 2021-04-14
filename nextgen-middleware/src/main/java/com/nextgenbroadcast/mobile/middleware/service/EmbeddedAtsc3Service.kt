package com.nextgenbroadcast.mobile.middleware.service

import android.os.Binder
import android.os.IBinder
import com.nextgenbroadcast.mobile.core.asReadOnly
import com.nextgenbroadcast.mobile.core.model.*
import com.nextgenbroadcast.mobile.core.presentation.*
import com.nextgenbroadcast.mobile.core.presentation.media.IObservablePlayer
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import kotlinx.coroutines.flow.*

class EmbeddedAtsc3Service : Atsc3ForegroundService() {

    override fun createServiceBinder(serviceController: IServiceController): IBinder =
            ServiceBinder(serviceController)

    internal inner class ServiceBinder(
            private val serviceController: IServiceController
    ) : Binder(), IServiceBinder {
        override val receiverPresenter: IReceiverPresenter = object : IReceiverPresenter {
            override val receiverState = serviceController.receiverState.asReadOnly()
            override val freqKhz = serviceController.receiverFrequency.asReadOnly()

            override fun openRoute(path: String): Boolean {
                openRoute(this@EmbeddedAtsc3Service, path)
                return true
            }

            override fun closeRoute() {
                closeRoute(this@EmbeddedAtsc3Service)
            }

            override fun tune(frequency: PhyFrequency) {
                serviceController.tune(frequency)
            }
        }

        override val selectorPresenter: ISelectorPresenter = object : ISelectorPresenter {
            override val sltServices = serviceController.routeServices.asReadOnly()
            override val selectedService = serviceController.selectedService.asReadOnly()

            override fun selectService(service: AVService): Boolean {
                return serviceController.selectService(service)
            }
        }

        override val userAgentPresenter = object : IUserAgentPresenter {
            private val viewController = requireViewController()

            override val appData = viewController.appData.asReadOnly()
            override val appState = viewController.appState.asReadOnly()

            override fun setApplicationState(state: ApplicationState) {
                viewController.setApplicationState(state)
            }
        }

        override val mediaPlayerPresenter = object : IMediaPlayerPresenter {
            private val viewController = requireViewController()

            override val rmpLayoutParams = viewController.rmpLayoutParams.asReadOnly()
            override val rmpMediaUri = viewController.rmpMediaUri.asReadOnly()

            override fun rmpLayoutReset() {
                viewController.rmpLayoutReset()
            }

            override fun rmpPlaybackChanged(state: PlaybackState) {
                viewController.rmpPlaybackChanged(state)
            }

            override fun rmpPlaybackRateChanged(speed: Float) {
                viewController.rmpPlaybackRateChanged(speed)
            }

            override fun rmpMediaTimeChanged(currentTime: Long) {
                viewController.rmpMediaTimeChanged(currentTime)
            }

            override fun addOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener) {
                viewController.addOnPlayerSateChangedCallback(callback)
            }

            override fun removeOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener) {
                viewController.removeOnPlayerSateChangedCallback(callback)
            }
        }

        override val controllerPresenter = object : IControllerPresenter {
            override fun setTelemetryEnabled(enabled: Boolean) {
                if (enabled) {
                    telemetryBroker?.start()
                } else {
                    telemetryBroker?.stop()
                }
            }

            override fun setTelemetryEnabled(type: String, enabled: Boolean) {
                telemetryBroker?.setReaderEnabled(type, enabled)
            }

            override fun setTelemetryUpdateDelay(type: String, delayMils: Long) {
                telemetryBroker?.setReaderDelay(type, delayMils)
            }

            override fun debugInfoSettings(): SharedFlow<Map<String, Boolean>> {
                return debugInfoSettings.asSharedFlow()
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