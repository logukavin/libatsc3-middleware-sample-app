package com.nextgenbroadcast.mobile.middleware.service

import android.os.Binder
import android.os.IBinder
import androidx.core.content.FileProvider
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.core.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.core.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.core.presentation.IUserAgentPresenter
import com.nextgenbroadcast.mobile.middleware.IMediaFileProvider
import java.io.File

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

            override fun createMMTSource() = serviceController.createMMTSource()
        }
        override val selectorPresenter: ISelectorPresenter = serviceController
        override val userAgentPresenter: IUserAgentPresenter = viewController
        override val mediaPlayerPresenter: IMediaPlayerPresenter = viewController
    }

    override fun getFileProvider() = object : IMediaFileProvider {

        override fun getFileProviderUri(path: String) = FileProvider.getUriForFile(
                applicationContext,
                "com.nextgenbroadcast.mobile.middleware.embedded.provider",
                File(path))
    }

    companion object {
        fun init() {
            clazz = EmbeddedAtsc3Service::class.java
        }
    }
}