package com.nextgenbroadcast.mobile.middleware.service

import android.os.*
import com.nextgenbroadcast.mobile.middleware.service.handler.Atsc3ServiceIncomingHandler
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter

class StandaloneAtsc3Service : Atsc3ForegroundService() {

    override fun createServiceBinder(serviceController: IServiceController, viewController: IViewController): IBinder =
            Messenger(Atsc3ServiceIncomingHandler(
                    lifecycleOwner = this@StandaloneAtsc3Service,
                    receiverPresenter = object : IReceiverPresenter {
                        override val receiverState = serviceController.receiverState

                        override fun openRoute(path: String): Boolean {
                            openRoute(this@StandaloneAtsc3Service, path)
                            return true
                        }

                        override fun closeRoute() {
                            closeRoute(this@StandaloneAtsc3Service)
                        }
                    },
                    serviceController = serviceController,
                    viewController = viewController
            )).binder

    companion object {
        init {
            clazz = StandaloneAtsc3Service::class.java
        }
    }
}