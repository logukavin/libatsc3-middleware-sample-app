package com.nextgenbroadcast.mobile.middleware.server.web

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.nextgenbroadcast.mobile.core.cert.CertificateUtils
import com.nextgenbroadcast.mobile.core.cert.IUserAgentSSLContext
import com.nextgenbroadcast.mobile.core.md5
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.gateway.web.ConnectionType
import com.nextgenbroadcast.mobile.middleware.gateway.web.IWebGateway
import com.nextgenbroadcast.mobile.middleware.server.ServerConstants.ATSC_CMD_PATH
import com.nextgenbroadcast.mobile.middleware.server.ws.MiddlewareWebSocket
import kotlinx.coroutines.*
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
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class MiddlewareWebServer constructor(
        private val server: Server,
        private val webGateway: IWebGateway?,
        private val globalScope: CoroutineScope
) : AutoCloseable, LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val availResources = arrayListOf<AppResource>()

    init {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        webGateway?.appCache?.observe(this, { applications ->
            onCacheChanged(applications)
        })
    }

    fun isRunning() = server.isRunning

    @Throws(MiddlewareWebServerError::class)
    fun start(generatedSSLContext: IUserAgentSSLContext?) {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        globalScope.launch {
            webGateway?.let { gateway ->
                server.connectors = gateway.hostName.let { hostName ->
                    ArrayList<Connector>().apply {
                        add(getServerConnector(ConnectionType.HTTP, server, hostName, gateway.httpPort))
                        add(getServerConnector(ConnectionType.WS, server, hostName, gateway.wsPort))

                        generatedSSLContext?.let { generatedSSLContext ->
                            val sslContextFactory = suspendCoroutine<SslContextFactory> { cor ->
                                CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()).launch {
                                    @Suppress("BlockingMethodInNonBlockingContext")
                                    cor.resume(configureSSLFactory(generatedSSLContext))
                                }
                            }
                            add(getSecureServerConnector(ConnectionType.HTTPS, server, hostName, gateway.httpsPort, sslContextFactory))
                            add(getSecureServerConnector(ConnectionType.WSS, server, hostName, gateway.wssPort, sslContextFactory))
                        }
                    }.toTypedArray()
                }
            }

            server.start()
            withContext(Dispatchers.Main) {
                setSelectedPorts(server.connectors.asList())
            }
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
        }.distinct()

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
        private var rpcGateway: IRPCGateway? = null
        private var webGateway: IWebGateway? = null

        fun rpcGateway(value: IRPCGateway) = apply { rpcGateway = value }

        fun webGateway(value: IWebGateway) = apply { webGateway = value }

        fun build(): MiddlewareWebServer {
            val server = Server()

            val handlerArray = ArrayList<Handler>().apply {
                rpcGateway?.let { rpcGateway ->
                    add(object : WebSocketHandler() {
                        override fun configure(factory: WebSocketServletFactory) {
                            factory.creator = WebSocketCreator { req, resp ->
                                createWebSocket(req, rpcGateway) ?: resp.sendError(404, "Not Found")
                            }
                        }
                    })
                }
            }.toTypedArray()

            with(server) {
                handler = HandlerList().apply {
                    handlers = handlerArray
                }
            }

            return MiddlewareWebServer(server, webGateway, GlobalScope)
        }
    }

    private fun setSelectedPorts(connectors: List<Connector>) {
        webGateway?.let { gateway ->
            connectors.forEach { connector ->
                (connector as? ServerConnector?)?.also { serverConnector ->
                    when (ConnectionType.valueOf(serverConnector.name)) {
                        ConnectionType.HTTP -> gateway.httpPort = serverConnector.localPort
                        ConnectionType.WS -> gateway.wsPort = serverConnector.localPort
                        ConnectionType.HTTPS -> gateway.httpsPort = serverConnector.localPort
                        ConnectionType.WSS -> gateway.wssPort = serverConnector.localPort
                    }
                }
            }
        }
    }
}

private fun createWebSocket(req: ServletUpgradeRequest, rpcGateway: IRPCGateway): WebSocketAdapter? {
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

fun getServerConnector(connectionType: ConnectionType, server: Server, serverHost: String, serverPort: Int): ServerConnector {
    return ServerConnector(server).apply {
        name = connectionType.type
        port = serverPort
        host = serverHost
    }
}

fun getSecureServerConnector(connectionType: ConnectionType, server: Server, serverHost: String, serverPort: Int, sslContextFactory: SslContextFactory): ServerConnector {
    val config = HttpConfiguration().apply {
        addCustomizer(SecureRequestCustomizer())
        securePort = serverPort
    }

    // Configuring the secure connector
    return ServerConnector(server, SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString()), HttpConnectionFactory(config)).apply {
        name = connectionType.type
        port = serverPort
        host = serverHost
    }
}
