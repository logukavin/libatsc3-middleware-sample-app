package com.nextgenbroadcast.mobile.middleware.server.web

import android.util.Log
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.core.md5
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.gateway.web.ConnectionType
import com.nextgenbroadcast.mobile.middleware.gateway.web.IWebGateway
import com.nextgenbroadcast.mobile.middleware.rpc.processor.CommandRPCProcessor
import com.nextgenbroadcast.mobile.middleware.rpc.processor.CompanionRPCProcessor
import com.nextgenbroadcast.mobile.middleware.server.CompanionServerConstants
import com.nextgenbroadcast.mobile.middleware.server.MiddlewareApplicationSession
import com.nextgenbroadcast.mobile.middleware.server.ServerConstants
import com.nextgenbroadcast.mobile.middleware.server.cert.IUserAgentSSLContext
import com.nextgenbroadcast.mobile.middleware.server.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.middleware.server.servlets.CDApplicationInfoServlet
import com.nextgenbroadcast.mobile.middleware.server.servlets.CDDescriptionServlet
import com.nextgenbroadcast.mobile.middleware.server.ws.MiddlewareWebSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.handler.ContextHandler
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
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class MiddlewareWebServer(
    private val server: Server,
    private val webGateway: IWebGateway?,
    private val companionServerHost: String?,
    private val companionServerPort: Int?,
    private val stateScope: CoroutineScope?
) : IMiddlewareWebServer, AutoCloseable {

    private val availResources = arrayListOf<AppResource>()

    init {
        stateScope?.launch {
            webGateway?.appCache?.collect { applications ->
                onCacheChanged(applications)
            }
        }
    }

    fun isRunning() = server.isRunning

    override fun addConnection(type: ConnectionType, host: String, port: Int) {
        try {
            val connector = server.connectors.filterIsInstance(ServerConnector::class.java).firstOrNull { connector ->
                connector.name == type.type && connector.host == host && connector.port == port
            } ?: getServerConnector(type, server, host, port).also {
                server.addConnector(it)
            }

            if (!connector.isRunning) {
                connector.start()
            }
        } catch (e: IOException) {
            LOG.e(TAG, "Failed to add web server connection to type: $type, host: $host, port: $port", e)
        }
    }

    override fun addHandler(path: String, onGet: (req: HttpServletRequest, resp: HttpServletResponse) -> Unit): Boolean {
        val handlerCollection = server.handler as? HandlerCollection ?: return false

        val servlet = object : HttpServlet() {
            override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
                onGet(req, resp)
            }
        }

        val contextHandler = handlerCollection.handlers.filterIsInstance(ContextHandler::class.java).firstOrNull { handler ->
            handler.contextPath == "/$path"
        } ?: ServletContextHandler().apply {
            contextPath = "/$path"
            addServlet(ServletHolder(servlet), "/*")
        }.also {
            handlerCollection.addHandler(it)
        }

        if (!contextHandler.isRunning) {
            contextHandler.start()
        }

        return true
    }

    override fun removeHandler(path: String) {
        val handlerCollection = server.handler as? HandlerCollection ?: return

        val contextHandler = handlerCollection.handlers.filterIsInstance(ContextHandler::class.java).firstOrNull { handler ->
            handler.contextPath == "/$path"
        } ?: return

        contextHandler.stop()
        handlerCollection.removeHandler(contextHandler)
    }

    @Throws(MiddlewareWebServerError::class)
    suspend fun start(generatedSSLContext: IUserAgentSSLContext?) {
        val serverConnectors = mutableListOf<Connector>()

        if (companionServerHost != null && companionServerPort != null) {
            serverConnectors.apply {
                add(getServerConnector(ConnectionType.HTTP, server, companionServerHost, companionServerPort))
            }
        }

        webGateway?.let { gateway ->
            gateway.hostName.let { hostName ->
                serverConnectors.apply {
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

        server.connectors = serverConnectors.toTypedArray()

        retry(RETRY_COUNT, "Can't start web server") {
            server.start()
        }

        val connectors = server.connectors.asList()
        withContext(Dispatchers.Main) {
            setSelectedPorts(connectors)
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

    @Throws(Exception::class)
    fun stop() {
        retry(RETRY_COUNT, "Can't stop web server") {
            server.stop()
        }
    }

    @Throws(Exception::class)
    override fun close() {
        stop()
    }

    private fun retry(count: Int, errorMessage: String, action: () -> Unit) {
        var attempts = count
        while (attempts >= 0) {
            try {
                action()
            } catch (e: Exception) {
                Log.d(TAG, "$errorMessage, attempt: ${count - attempts}", e)
            }

            attempts--
        }
    }

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
        private var stateScope: CoroutineScope? = null
        private var rpcGateway: IRPCGateway? = null
        private var webGateway: IWebGateway? = null
        private var companionServerHost: String? = null
        private var companionServerPort: Int? = null

        fun stateScope(value: CoroutineScope) = apply { stateScope = value }

        fun rpcGateway(value: IRPCGateway) = apply { rpcGateway = value }

        fun webGateway(value: IWebGateway) = apply { webGateway = value }

        fun companionServer(serverHost: String, serverPort: Int) = apply {
            companionServerHost = serverHost
            companionServerPort = serverPort
        }

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

                if (companionServerHost != null && companionServerPort != null) {
                    add(initServletsHandler())
                }

            }.toTypedArray()

            with(server) {
                handler = HandlerList().apply {
                    handlers = handlerArray
                }
            }

            return MiddlewareWebServer(server, webGateway, companionServerHost, companionServerPort, stateScope)
        }

        private fun initServletsHandler(): Handler {
            val applicationInfoServletHandler = ServletContextHandler().apply {
                contextPath = "/"
                addServlet(ServletHolder(CDApplicationInfoServlet()), CompanionServerConstants.APPLICATION_INFO_PATH)
                addServlet(ServletHolder(CDDescriptionServlet("")), CompanionServerConstants.DEVICE_DESCRIPTION_PATH)
            }

            return applicationInfoServletHandler
        }
    }

    companion object {
        val TAG: String = MiddlewareWebServer::class.java.simpleName

        private const val RETRY_COUNT = 1
    }
}

private fun createWebSocket(req: ServletUpgradeRequest, rpcGateway: IRPCGateway): WebSocketAdapter? {
    return when (req.httpServletRequest.pathInfo) {
        ServerConstants.ATSC_CMD_PATH ->  {
            val session = MiddlewareApplicationSession(rpcGateway)
            MiddlewareWebSocket(session, CommandRPCProcessor(session))
        }

        ServerConstants.ATSC_CD_PATH -> {
            val session = MiddlewareApplicationSession(rpcGateway)
            MiddlewareWebSocket(session, CompanionRPCProcessor(session))
        }

        else -> null
    }
}

@Throws(GeneralSecurityException::class, IOException::class)
fun configureSSLFactory(generatedSSLContext: IUserAgentSSLContext): SslContextFactory {
    val password = UUID.randomUUID().toString() // create password for one session
    // Configuring SSL
    return SslContextFactory.Server().apply {
        keyStoreType = UserAgentSSLContext.KEY_STORE_TYPE
        sslContext = generatedSSLContext.getInitializedSSLContext(password)
        setKeyStorePassword(password)
        setKeyManagerPassword(password)
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
