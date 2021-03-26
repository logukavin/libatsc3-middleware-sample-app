package com.nextgenbroadcast.mobile.middleware.service

import android.content.Intent
import android.os.*
import com.nextgenbroadcast.mobile.core.asReadOnly
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.middleware.service.handler.StandaloneServiceHandler
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter

class StandaloneAtsc3Service : Atsc3ForegroundService() {

    private var serviceHandler: StandaloneServiceHandler? = null

    override fun createServiceBinder(serviceController: IServiceController): IBinder {
        return Messenger(
                StandaloneServiceHandler(
                        receiverPresenter = object : IReceiverPresenter {
                            override val receiverState = serviceController.receiverState.asReadOnly()
                            override val freqKhz = serviceController.receiverFrequency.asReadOnly()

                            override fun openRoute(path: String): Boolean {
                                openRoute(this@StandaloneAtsc3Service, path)
                                return true
                            }

                            override fun closeRoute() {
                                closeRoute(this@StandaloneAtsc3Service)
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

    override fun onUnbind(intent: Intent): Boolean {
        // Don't clear by provider action
        if (intent.action == null) {
            serviceHandler?.unSubscribeAll()
        }
        return super.onUnbind(intent)
    }

    companion object {
        init {
            clazz = StandaloneAtsc3Service::class.java
        }
    }
}