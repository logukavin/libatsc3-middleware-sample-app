package com.nextgenbroadcast.mobile.middleware.service

import android.os.Binder
import android.os.IBinder
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.presentation.*
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController

class EmbeddedAtsc3Service : Atsc3ForegroundService() {

    override fun createServiceBinder(serviceController: IServiceController): IBinder =
            ServiceBinder(serviceController)

    override fun createProviderServiceBinder(serviceController: IServiceController): IBinder =
            ProviderServiceBinder(serviceController)

    inner class ServiceBinder(
            private val serviceController: IServiceController
    ) : Binder(), IServiceBinder {
        override val receiverPresenter: IReceiverPresenter = object : IReceiverPresenter {
            override val receiverState = serviceController.receiverState
            override val freqKhz = serviceController.freqKhz

            override fun openRoute(path: String): Boolean {
                openRoute(this@EmbeddedAtsc3Service, path)
                return true
            }

            override fun closeRoute() {
                closeRoute(this@EmbeddedAtsc3Service)
            }

            override fun createMMTSource() = serviceController.createMMTSource()
            override fun tune(frequency: PhyFrequency) {
                serviceController.tune(frequency)
            }
        }
        override val selectorPresenter: ISelectorPresenter
            get() = serviceController
        override val userAgentPresenter: IUserAgentPresenter
            get() = requireViewController()
        override val mediaPlayerPresenter: IMediaPlayerPresenter
            get() = requireViewController()
    }

    internal inner class ProviderServiceBinder (
            val serviceController: IServiceController
    ) : Binder()

    companion object {
        fun init() {
            clazz = EmbeddedAtsc3Service::class.java
        }
    }
}