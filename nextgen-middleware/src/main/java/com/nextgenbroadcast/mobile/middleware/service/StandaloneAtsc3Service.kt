package com.nextgenbroadcast.mobile.middleware.service

import android.content.Intent
import android.os.*
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.middleware.service.provider.StandaloneMediaFileProvider
import com.nextgenbroadcast.mobile.middleware.service.handler.StandaloneServiceHandler
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.mmt.atsc3.media.MMTDataBuffer
import java.lang.UnsupportedOperationException

class StandaloneAtsc3Service : Atsc3ForegroundService() {

    override val mediaFileProvider by lazy {
        StandaloneMediaFileProvider(applicationContext)
    }

    private var serviceHandler: StandaloneServiceHandler? = null

    override fun createProviderServiceBinder(serviceController: IServiceController): IBinder? = null

    override fun createActivityServiceBinder(serviceController: IServiceController): IBinder {
        return Messenger(
                StandaloneServiceHandler(
                        mediaFileProvider,
                        lifecycleOwner = this@StandaloneAtsc3Service,
                        receiverPresenter = object : IReceiverPresenter {
                            override val receiverState = serviceController.receiverState
                            override val freqKhz = serviceController.freqKhz

                            override fun openRoute(path: String): Boolean {
                                openRoute(this@StandaloneAtsc3Service, path)
                                return true
                            }

                            override fun closeRoute() {
                                closeRoute(this@StandaloneAtsc3Service)
                            }

                            override fun createMMTSource(): MMTDataBuffer {
                                throw UnsupportedOperationException("MMT playback is not supported with standalone service")
                            }

                            override fun tune(frequency: PhyFrequency) {
                                serviceController.tune(frequency)
                            }
                        },
                        serviceController = serviceController,
                        requireViewController = ::requireViewController
                ).also {
                    serviceHandler = it
                }
        ).binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        serviceHandler?.unSubscribeAll()
        return super.onUnbind(intent)
    }

    companion object {
        init {
            clazz = StandaloneAtsc3Service::class.java
        }
    }
}