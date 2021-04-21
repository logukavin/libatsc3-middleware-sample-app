package com.nextgenbroadcast.mobile.middleware.service

import android.os.Binder
import android.os.IBinder
import com.nextgenbroadcast.mobile.core.asReadOnly
import com.nextgenbroadcast.mobile.core.model.*
import com.nextgenbroadcast.mobile.core.presentation.*
import com.nextgenbroadcast.mobile.core.presentation.media.IObservablePlayer
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.server.cert.UserAgentSSLContext
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
            private val sslContext = UserAgentSSLContext.newInstance(applicationContext)

            override val appData = viewController.appData.asReadOnly()
            override val appState = viewController.appState.asReadOnly()

            override fun setApplicationState(state: ApplicationState) {
                viewController.setApplicationState(state)
            }

            override fun getWebServerCertificateHash(): String? {
                return sslContext.getCertificateHash()
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
            //TODO: isolate Flow to prevent internal objects blocking with UI
            // maybe something like this: .stateIn(viewController.scope(), SharingStarted.Lazily, telemetryBroker.readersEnabled.value)
            override val telemetryEnabled = telemetryHolder.telemetryEnabled.asReadOnly()
            override val telemetryDelay = telemetryHolder.telemetryDelay.asReadOnly()
            override val debugInfoSettings = telemetryHolder.debugInfoSettings.asReadOnly()

            override fun setTelemetryEnabled(enabled: Boolean) {
                telemetryHolder.setAllEnabled(enabled)
            }

            override fun setTelemetryEnabled(type: String, enabled: Boolean) {
                telemetryHolder.setTelemetryEnabled(enabled, type)
            }

            override fun setTelemetryUpdateDelay(type: String, delayMils: Long) {
                telemetryHolder.setTelemetryDelay(delayMils, type)
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