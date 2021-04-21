package com.nextgenbroadcast.mobile.middleware.service.holder

import android.content.Context
import com.nextgenbroadcast.mobile.middleware.Atsc3ReceiverCore
import com.nextgenbroadcast.mobile.middleware.server.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.middleware.server.web.IMiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.server.web.MiddlewareWebServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class WebServerHolder(
        private val context: Context,
        private val receiver: Atsc3ReceiverCore,
        private val onCreated: (server: IMiddlewareWebServer) -> Unit = {},
        private val onDestroyed: () -> Unit = {}
) {
    private var webServer: MiddlewareWebServer? = null

    fun start() {
        val (web, rpc, stateScope) = receiver.getWebInterface() ?: return

        val sslContext = UserAgentSSLContext.newInstance(context.applicationContext)

        webServer = MiddlewareWebServer.Builder()
                .stateScope(stateScope)
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

    fun stop() {
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
    }
}