package com.nextgenbroadcast.mobile.middleware.service

import android.net.Uri
import android.os.Binder
import android.os.IBinder
import com.nextgenbroadcast.mobile.core.asReadOnly
import com.nextgenbroadcast.mobile.core.model.*
import com.nextgenbroadcast.mobile.core.presentation.*
import com.nextgenbroadcast.mobile.core.presentation.media.IObservablePlayer
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.Atsc3ReceiverCore
import com.nextgenbroadcast.mobile.middleware.MiddlewareConfig
import com.nextgenbroadcast.mobile.middleware.atsc3.source.IAtsc3Source
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.server.cert.UserAgentSSLContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Deprecated("Use ReceiverContentProvider instead")
class EmbeddedAtsc3Service : Atsc3ForegroundService() {

    override fun createServiceBinder(receiver: Atsc3ReceiverCore): IBinder =
            ServiceBinder(/*receiver.serviceController*/object : IServiceController {
                override val receiverState: StateFlow<ReceiverState>
                    get() = TODO("Not yet implemented")
                override val receiverFrequency: StateFlow<Int>
                    get() = TODO("Not yet implemented")
                override val routeServices: StateFlow<List<AVService>>
                    get() = TODO("Not yet implemented")
                override val errorFlow: SharedFlow<String>
                    get() = TODO("Not yet implemented")

                override suspend fun openRoute(source: IAtsc3Source, force: Boolean): Boolean {
                    TODO("Not yet implemented")
                }

                override suspend fun closeRoute() {
                    TODO("Not yet implemented")
                }

                override suspend fun tune(frequency: PhyFrequency) {
                    TODO("Not yet implemented")
                }

                override suspend fun selectService(service: AVService): Boolean {
                    TODO("Not yet implemented")
                }

                override suspend fun cancelScanning() {
                    TODO("Not yet implemented")
                }

                override fun findServiceById(globalServiceId: String): AVService? {
                    TODO("Not yet implemented")
                }

                override fun getNearbyService(offset: Int): AVService? {
                    TODO("Not yet implemented")
                }

                override fun getCurrentService(): AVService? {
                    TODO("Not yet implemented")
                }

                override fun getCurrentRouteMediaUrl(): MediaUrl? {
                    TODO("Not yet implemented")
                }
            })

    internal inner class ServiceBinder(
            private val serviceController: IServiceController
    ) : Binder(), IServiceBinder {
        override val receiverPresenter: IReceiverPresenter = object : IReceiverPresenter {
            override val receiverState = MutableStateFlow(ReceiverState.idle())//serviceController.receiverState.asReadOnly()
            override val freqKhz = MutableStateFlow(0)//serviceController.receiverFrequency.asReadOnly()

            override fun openRoute(path: String): Boolean {
                openRoute(this@EmbeddedAtsc3Service, path)
                return true
            }

            override fun closeRoute() {
                closeRoute(this@EmbeddedAtsc3Service)
            }

            override fun tune(frequency: PhyFrequency) {
//                CoroutineScope(Dispatchers.Default).launch {
//                    serviceController.tune(frequency)
//                }
            }
        }

        override val selectorPresenter: ISelectorPresenter = object : ISelectorPresenter {
            override val sltServices = MutableStateFlow(emptyList<AVService>())//serviceController.routeServices.asReadOnly()
            override val selectedService = MutableStateFlow<AVService?>(null)//serviceController.selectedService.asReadOnly()

            override fun selectService(service: AVService) {
//                CoroutineScope(Dispatchers.Default).launch {
//                    serviceController.selectService(service)
//                }
            }
        }

        override val userAgentPresenter = object : IUserAgentPresenter {
            //private val viewController = requireViewController()
            private val sslContext = UserAgentSSLContext.newInstance(applicationContext)

            override val appData = MutableStateFlow<AppData?>(null)//viewController.appData.asReadOnly()

            override fun setApplicationState(state: ApplicationState) {
                //viewController.setApplicationState(state)
            }

            override fun getWebServerCertificateHash(): String? {
                return sslContext.getCertificateHash()
            }
        }

        override val mediaPlayerPresenter = object : IMediaPlayerPresenter {
            //private val viewController = requireViewController()

            override val rmpLayoutParams = MutableStateFlow(RPMParams())//viewController.rmpLayoutParams.asReadOnly()
            override val rmpMediaUri = MutableStateFlow<Uri?>(null)//viewController.rmpMediaUri.asReadOnly()

            override fun rmpLayoutReset() {
                //viewController.rmpLayoutReset()
            }

            override fun rmpPlaybackChanged(state: PlaybackState) {
                //viewController.rmpPlaybackChanged(state)
            }

            override fun rmpPlaybackRateChanged(speed: Float) {
                //viewController.rmpPlaybackRateChanged(speed)
            }

            override fun rmpMediaTimeChanged(currentTime: Long) {
                //viewController.rmpMediaTimeChanged(currentTime)
            }

            override fun addOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener) {
                //viewController.addOnPlayerSateChangedCallback(callback)
            }

            override fun removeOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener) {
                //viewController.removeOnPlayerSateChangedCallback(callback)
            }
        }

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