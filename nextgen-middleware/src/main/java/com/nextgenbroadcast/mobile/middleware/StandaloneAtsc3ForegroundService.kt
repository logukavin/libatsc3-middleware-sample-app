package com.nextgenbroadcast.mobile.middleware

import android.content.Intent
import android.os.*
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter

class StandaloneAtsc3ForegroundService: Atsc3ForegroundService() {

    private val messenger: Messenger by lazy {
        Messenger(Atsc3ServiceIncomingHandler(
            lifecycleOwner = this@StandaloneAtsc3ForegroundService,
            receiverPresenter = object : IReceiverPresenter {
                override val receiverState = serviceController.receiverState

                override fun openRoute(path: String): Boolean {
                    openRoute(this@StandaloneAtsc3ForegroundService, path)
                    return true
                }

                override fun closeRoute() {
                    closeRoute(this@StandaloneAtsc3ForegroundService)
                }
            },
            serviceController = serviceController,
            viewController = requireViewController()
        ))
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)

        return messenger.binder
    }
}