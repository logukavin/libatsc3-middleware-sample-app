package com.nextgenbroadcast.mobile.middleware

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.nextgenbroadcast.mobile.middleware.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IUserAgentPresenter

class LinkedAtsc3ForegroundService : Atsc3ForegroundService() {

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)

        return ServiceBinder()
    }

    inner class ServiceBinder : Binder(), IServiceBinder {
        override val receiverPresenter: IReceiverPresenter = object : IReceiverPresenter {
            override val receiverState = serviceController.receiverState

            override fun openRoute(path: String): Boolean {
                openRoute(this@LinkedAtsc3ForegroundService, path)
                return true
            }

            override fun closeRoute() {
                closeRoute(this@LinkedAtsc3ForegroundService)
            }
        }
        override val selectorPresenter: ISelectorPresenter = serviceController
        override val userAgentPresenter: IUserAgentPresenter = requireViewController()
        override val mediaPlayerPresenter: IMediaPlayerPresenter = requireViewController()
    }
}