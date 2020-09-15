package com.nextgenbroadcast.mobile.middleware.service

import android.os.Binder
import android.os.IBinder
import com.nextgenbroadcast.mobile.middleware.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IUserAgentPresenter

class EmbeddedAtsc3Service : Atsc3ForegroundService() {

    override fun createServiceBinder(serviceController: IServiceController, viewController: IViewController): IBinder =
            ServiceBinder(serviceController, viewController)

    inner class ServiceBinder(
            private val serviceController: IServiceController,
            private val viewController: IViewController
    ) : Binder(), IServiceBinder {
        override val receiverPresenter: IReceiverPresenter = object : IReceiverPresenter {
            override val receiverState = serviceController.receiverState

            override fun openRoute(path: String): Boolean {
                openRoute(this@EmbeddedAtsc3Service, path)
                return true
            }

            override fun closeRoute() {
                closeRoute(this@EmbeddedAtsc3Service)
            }
        }
        override val selectorPresenter: ISelectorPresenter = serviceController
        override val userAgentPresenter: IUserAgentPresenter = viewController
        override val mediaPlayerPresenter: IMediaPlayerPresenter = viewController
    }

    companion object {
        fun init() {
            clazz = EmbeddedAtsc3Service::class.java
        }
    }
}