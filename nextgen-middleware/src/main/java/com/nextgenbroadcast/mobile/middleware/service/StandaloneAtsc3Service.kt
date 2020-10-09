package com.nextgenbroadcast.mobile.middleware.service

import android.content.Intent
import android.os.IBinder
import android.os.Messenger
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.service.handler.StandaloneServiceHandler
import com.nextgenbroadcast.mobile.mmt.atsc3.media.MMTDataBuffer

class StandaloneAtsc3Service : Atsc3ForegroundService() {

    private var serviceHandler: StandaloneServiceHandler? = null

    override fun createServiceBinder(serviceController: IServiceController, viewController: IViewController): IBinder {
        serviceHandler = StandaloneServiceHandler(
                mediaFileProvider,
                lifecycleOwner = this@StandaloneAtsc3Service,
                receiverPresenter = object : IReceiverPresenter {

                    override val freqKhz: Int
                        get() = TODO("Not yet implemented")

                    override fun tune(freqKhz: Int) {
                        TODO("Not yet implemented")
                    }

                    override val receiverState = serviceController.receiverState

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
                },
                serviceController = serviceController,
                viewController = viewController
        )
        return Messenger(serviceHandler).binder
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