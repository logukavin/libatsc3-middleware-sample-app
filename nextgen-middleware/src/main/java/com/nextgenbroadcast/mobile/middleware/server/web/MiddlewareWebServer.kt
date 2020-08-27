package com.nextgenbroadcast.mobile.middleware.server.web

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.nextgenbroadcast.mobile.core.cert.CertificateUtils
import com.nextgenbroadcast.mobile.core.cert.IUserAgentSSLContext
import com.nextgenbroadcast.mobile.core.md5
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.gateway.web.IWebGateway
import com.nextgenbroadcast.mobile.middleware.server.ServerConstants.ATSC_CMD_PATH
import com.nextgenbroadcast.mobile.middleware.server.ws.MiddlewareWebSocket
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.eclipse.jetty.websocket.server.WebSocketHandler
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.*


class MiddlewareWebServer constructor(
        private val server: Server,
        webGateway: IWebGateway?
) : AutoCloseable, LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val availResources = arrayListOf<AppResource>()

    init {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        webGateway?.appCache?.observe(this, androidx.lifecycle.Observer { applications ->
            onCacheChanged(applications)
        })
    }

    fun isRunning() = server.isRunning

    @Throws(MiddlewareWebServerError::class)
    fun start() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        GlobalScope.launch {
            server.start()
        }
    }

    @Throws(Exception::class)
    fun stop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        server.stop()
    }

    @Throws(Exception::class)
    override fun close() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        stop()
    }

    override fun getLifecycle() = lifecycleRegistry

    private fun onCacheChanged(applications: List<Atsc3Application>) {
        val handler = server.handler
        if (handler !is HandlerCollection) return

        val resources = applications.flatMap { app ->
            app.appContextIdList.map { contextId ->
                AppResource(contextId, app.cachePath)
            }
        }

        val oldResources = availResources.subtract(resources)
        removeResources(handler, oldResources)
        availResources.removeAll(oldResources)

        val newResources = resources.subtract(availResources)
        addResources(handler, newResources)
        availResources.addAll(newResources)
    }

    private fun removeResources(handler: HandlerCollection, resources: Set<AppResource>) {
        val addedResources: List<ServletContextHandler> = handler.handlers.filterIsInstance<ServletContextHandler>()

        addedResources.filter { servlet ->
            val contextPath = servlet.contextPath.substring(1)
            resources.firstOrNull { it.contextId == contextPath } != null
        }.forEach { servlet ->
            servlet.stop()
            handler.removeHandler(servlet)
        }
    }

    private fun addResources(handler: HandlerCollection, resources: Set<AppResource>) {
        resources.forEach { resource ->
            val contextId = resource.contextId.md5()
            val contextHandler = ServletContextHandler().apply {
                contextPath = "/$contextId"
                baseResource = Resource.newResource(resource.cachePath)
                addServlet(ServletHolder(DefaultServlet()), "/")
            }
            handler.addHandler(contextHandler)
            contextHandler.start()
        }
    }

    private data class AppResource(
            val contextId: String,
            val cachePath: String
    )

    class Builder {
        private var httpsPort: Int? = null
        private var httpPort: Int? = null
        private var wssPort: Int? = null
        private var wsPort: Int? = null
        private var hostName: String? = null
        private var generatedSSLContext: IUserAgentSSLContext? = null
        private var rpcGateway: IRPCGateway? = null
        private var webGateway: IWebGateway? = null

        fun httpsPort(value: Int) = apply { httpsPort = value }

        fun httpPort(value: Int) = apply { httpPort = value }

        fun wssPort(value: Int) = apply { wssPort = value }

        fun wsPort(value: Int) = apply { wsPort = value }

        fun hostName(value: String) = apply { hostName = value }

        fun sslContext(value: IUserAgentSSLContext) = apply { generatedSSLContext = value }

        fun rpcGateway(value: IRPCGateway) = apply { rpcGateway = value }

        fun webGateway(value: IWebGateway) = apply { webGateway = value }

        fun build(): MiddlewareWebServer {
            val server = Server()

            val connectorArray = hostName?.let { hostName ->
                ArrayList<Connector>().apply {
                    httpPort?.let { port ->
                        add(getServerConnector(server, hostName, port))
                    }
                    wsPort?.let { port ->
                        add(getServerConnector(server, hostName, port))
                    }
                    generatedSSLContext?.let { generatedSSLContext ->
                        val sslContextFactory = configureSSLFactory(generatedSSLContext)

                        httpsPort?.let { port ->
                            add(getSecureServerConnector(server, hostName, port, sslContextFactory))
                        }
                        wssPort?.let { port ->
                            add(getSecureServerConnector(server, hostName, port, sslContextFactory))
                        }
                    }
                }.toTypedArray()
            }

            val handlerArray = ArrayList<Handler>().apply {
                rpcGateway?.let { rpcGateway ->
                    add(object : WebSocketHandler() {
                        override fun configure(factory: WebSocketServletFactory) {
                            factory.creator = WebSocketCreator { req, resp ->
                                createWebSocket(req, resp, rpcGateway) ?: resp.sendError(404, "Not Found")
                            }
                        }
                    })
                }
            }.toTypedArray()

            with(server) {
                connectors = connectorArray
                handler = HandlerList().apply {
                    handlers = handlerArray
                }
            }

            return MiddlewareWebServer(server, webGateway)
        }
    }
}

private fun createWebSocket(req: ServletUpgradeRequest, resp: ServletUpgradeResponse, rpcGateway: IRPCGateway): WebSocketAdapter? {
    return when (req.httpServletRequest.pathInfo) {
        ATSC_CMD_PATH -> MiddlewareWebSocket(rpcGateway)
        else -> null
    }
}

@Throws(GeneralSecurityException::class, IOException::class)
fun configureSSLFactory(generatedSSLContext: IUserAgentSSLContext): SslContextFactory {
    // Configuring SSL
    return SslContextFactory.Server().apply {
        keyStoreType = CertificateUtils.KEY_STORE_TYPE
        //TODO: remove hardcoded password
        sslContext = generatedSSLContext.getInitializedSSLContext("MY_PASSWORD")
        setKeyStorePassword("MY_PASSWORD")
        setKeyManagerPassword("MY_PASSWORD")
    }
}

fun getServerConnector(server: Server, serverHost: String, serverPort: Int): ServerConnector {
    return ServerConnector(server).apply {
        port = serverPort
        host = serverHost
    }
}

fun getSecureServerConnector(server: Server, serverHost: String, serverPort: Int, sslContextFactory: SslContextFactory): ServerConnector {
    val config = HttpConfiguration().apply {
        addCustomizer(SecureRequestCustomizer())
        securePort = serverPort
    }

    // Configuring the secure connector
    return ServerConnector(server, SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString()), HttpConnectionFactory(config)).apply {
        port = serverPort
        host = serverHost
    }
}
