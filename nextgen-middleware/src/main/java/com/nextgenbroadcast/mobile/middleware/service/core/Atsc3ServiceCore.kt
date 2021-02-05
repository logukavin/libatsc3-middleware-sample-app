package com.nextgenbroadcast.mobile.middleware.service.core

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.nextgenbroadcast.mobile.core.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.presentation.*
import com.nextgenbroadcast.mobile.middleware.analytics.Atsc3Analytics
import com.nextgenbroadcast.mobile.middleware.analytics.IAtsc3Analytics
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3Module
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.db.RoomServiceGuideStore
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.db.SGDataBase
import com.nextgenbroadcast.mobile.middleware.atsc3.source.IAtsc3Source
import com.nextgenbroadcast.mobile.middleware.cache.ApplicationCache
import com.nextgenbroadcast.mobile.middleware.cache.DownloadManager
import com.nextgenbroadcast.mobile.middleware.cache.IApplicationCache
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.service.ServiceControllerImpl
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.controller.view.ViewControllerImpl
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.RPCGatewayImpl
import com.nextgenbroadcast.mobile.middleware.gateway.web.IWebGateway
import com.nextgenbroadcast.mobile.middleware.gateway.web.WebGatewayImpl
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.repository.RepositoryImpl
import com.nextgenbroadcast.mobile.middleware.server.web.MiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.service.provider.IMediaFileProvider
import com.nextgenbroadcast.mobile.middleware.service.provider.MediaFileProvider
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.ESGContentAuthority
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import kotlinx.coroutines.Dispatchers

internal class Atsc3ServiceCore(
        val context: Context,
        val settings: IMiddlewareSettings
) : IAtsc3ServiceCore {
    private val repository: IRepository
    private val atsc3Module: Atsc3Module
    val serviceController: IServiceController
    private val appCache: IApplicationCache
    private val atsc3Analytics: IAtsc3Analytics

    var viewController: IViewController? = null
        private set
    private var webGateway: IWebGateway? = null
    private var rpcGateway: IRPCGateway? = null
    private var webServer: MiddlewareWebServer? = null

    private val mediaFileProvider: IMediaFileProvider by lazy {
        MediaFileProvider(context)
    }

    init {
        val repo = RepositoryImpl().also {
            repository = it
        }
        val atsc3 = Atsc3Module(context).also {
            atsc3Module = it
        }

        val sgDataBase = SGDataBase.getDatabase(context)
        val serviceGuideStore = RoomServiceGuideStore(sgDataBase).apply {
            subscribe {
                context.contentResolver.notifyChange(ESGContentAuthority.getServiceContentUri(context), null)
            }
        }

        atsc3Analytics = Atsc3Analytics.getInstance(context, settings)
        serviceController = ServiceControllerImpl(repo, serviceGuideStore, settings, atsc3, atsc3Analytics)

        appCache = ApplicationCache(atsc3.jni_getCacheDir(), DownloadManager())
    }

    fun destroy() {
        stopAndDestroyViewPresentation()
        atsc3Module.close()
    }

    fun createAndStartViewPresentation(lifecycleOwner: LifecycleOwner): IViewController {
        val view = ViewControllerImpl(repository, settings, mediaFileProvider, atsc3Analytics).apply {
            start(lifecycleOwner)
        }.also {
            viewController = it
        }
        val web = WebGatewayImpl(serviceController, repository, settings).also {
            webGateway = it
        }
        val rpc = RPCGatewayImpl(view, serviceController, appCache, settings, Dispatchers.Main, Dispatchers.IO).apply {
            start(lifecycleOwner)
        }.also {
            rpcGateway = it
        }

        view.appState.observe(lifecycleOwner) { appState ->
            when (appState) {
                ApplicationState.OPENED -> atsc3Analytics.startApplicationSession()
                ApplicationState.LOADED,
                ApplicationState.UNAVAILABLE -> atsc3Analytics.finishApplicationSession()
                else -> {
                    // ignore
                }
            }
        }

        startWebServer(rpc, web)

        return view;
    }

    fun stopAndDestroyViewPresentation() {
        stopWebServer()

        webGateway = null
        rpcGateway = null
        viewController = null
    }

    private fun startWebServer(rpc: IRPCGateway, web: IWebGateway) {
        webServer = MiddlewareWebServer.Builder()
                .rpcGateway(rpc)
                .webGateway(web)
                .build().also {
                    it.start(UserAgentSSLContext(context))
                }
    }

    private fun stopWebServer() {
        webServer?.let { server ->
            if (server.isRunning()) {
                try {
                    server.stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        webServer = null
    }

    override fun openRoute(filePath: String): Boolean {
        return serviceController.openRoute(filePath)
    }

    override fun openRoute(source: IAtsc3Source): Boolean {
        return serviceController.openRoute(source)
    }

    override fun closeRoute() {
        serviceController.stopRoute() // call to stopRoute is not a mistake. We use it to close previously opened file
        serviceController.closeRoute()
    }

    override fun tune(frequency: PhyFrequency) {
        serviceController.tune(frequency)
    }

    override fun getReceiverState(): ReceiverState {
        return serviceController.receiverState.value ?: ReceiverState.IDLE
    }
}