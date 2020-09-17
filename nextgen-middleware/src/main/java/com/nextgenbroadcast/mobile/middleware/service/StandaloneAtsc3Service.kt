package com.nextgenbroadcast.mobile.middleware.service

import android.os.*
import com.nextgenbroadcast.mobile.middleware.service.handler.StandaloneServiceHandler
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.mmt.atsc3.media.MMTDataSource
import java.lang.UnsupportedOperationException

class StandaloneAtsc3Service : Atsc3ForegroundService() {

    override fun createServiceBinder(serviceController: IServiceController, viewController: IViewController): IBinder =
            Messenger(StandaloneServiceHandler(
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

                        override fun createMMTSource(): MMTDataSource {
                            throw UnsupportedOperationException("MMT playback is not supported with standalone service")
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