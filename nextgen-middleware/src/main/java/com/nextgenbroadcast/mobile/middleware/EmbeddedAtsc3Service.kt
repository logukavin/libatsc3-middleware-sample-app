package com.nextgenbroadcast.mobile.middleware

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.nextgenbroadcast.mobile.middleware.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IUserAgentPresenter

class EmbeddedAtsc3Service : Atsc3ForegroundService() {

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)

        return ServiceBinder()
    }

    inner class ServiceBinder : Binder(), IServiceBinder {
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
        override val userAgentPresenter: IUserAgentPresenter = requireViewController()
        override val mediaPlayerPresenter: IMediaPlayerPresenter = requireViewController()
    }
}