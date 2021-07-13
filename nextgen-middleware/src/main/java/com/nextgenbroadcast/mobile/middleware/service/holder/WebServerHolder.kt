package com.nextgenbroadcast.mobile.middleware.service.holder

import android.content.Context
import androidx.annotation.MainThread
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.Atsc3ReceiverCore
import com.nextgenbroadcast.mobile.middleware.cache.ApplicationCache
import com.nextgenbroadcast.mobile.middleware.cache.DownloadManager
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.RPCGatewayImpl
import com.nextgenbroadcast.mobile.middleware.gateway.web.IWebGateway
import com.nextgenbroadcast.mobile.middleware.gateway.web.WebGatewayImpl
import com.nextgenbroadcast.mobile.middleware.server.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.middleware.server.web.IMiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.server.web.MiddlewareWebServer
import kotlinx.coroutines.*

internal class WebServerHolder(
        private val context: Context,
        private val receiver: Atsc3ReceiverCore,
        private val onCreated: (server: IMiddlewareWebServer) -> Unit = {},
        private val onDestroyed: () -> Unit = {}
) {
    private var webGateway: IWebGateway? = null
    private var rpcGateway: IRPCGateway? = null
    private var webServer: MiddlewareWebServer? = null
    private var stateScope: CoroutineScope? = null

    @MainThread
    fun open() {
        if (webServer != null) return

        val viewController = receiver.viewController
        val appContext = context.applicationContext
        val repository = receiver.repository
        val settings = receiver.settings
        val downloadManager = DownloadManager()
        val appCache = ApplicationCache(appContext.cacheDir, downloadManager)
        val sslContext = UserAgentSSLContext.newInstance(appContext)

        val scope = CoroutineScope(Dispatchers.Default).also {
            stateScope = it
        }
        val web = WebGatewayImpl(repository, settings).also {
            webGateway = it
        }
        val rpc = RPCGatewayImpl(repository, viewController, receiver.serviceController, appCache, settings, scope).also {
            rpcGateway = it
        }

        webServer = MiddlewareWebServer.Builder()
                .stateScope(scope)
                .rpcGateway(rpc)
                .webGateway(web)
                .build().also { server ->
                    GlobalScope.launch {
                        server.start(sslContext)

                        withContext(Dispatchers.Main) {
                            onCreated(server)
                        }
                    }
                }
    }

    @MainThread
    fun close() {
        try {
            stateScope?.cancel()
        } catch (e: IllegalStateException) {
            LOG.w(TAG, "Failed to close web server state scope", e)
        }
        stateScope = null

        webServer?.let { server ->
            if (server.isRunning()) {
                GlobalScope.launch {
                    try {
                        server.stop()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    withContext(Dispatchers.Main) {
                        onDestroyed()
                    }
                }
            }
        }

        webServer = null
        webGateway = null
        rpcGateway = null
    }

    companion object {
        val TAG: String = WebServerHolder::class.java.simpleName
    }
}