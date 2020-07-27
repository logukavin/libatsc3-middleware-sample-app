package org.ngbp.jsonrpc4jtestharness.core.web

import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.server.WebSocketHandler
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import org.ngbp.jsonrpc4jtestharness.core.cert.IUserAgentSSLContext
import org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWebSocket
import org.ngbp.jsonrpc4jtestharness.core.ws.SocketHolder
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCProcessor
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.*


class MiddlewareWebServer private constructor(builder: Builder) : AutoCloseable {

    private val httpsPort: Int
    private val httpPort: Int
    private val wssPort: Int
    private val wsPort: Int
    private val hostName: String?
    private val resourcePath: String?
    private val rpcProcessor: RPCProcessor?
    private val sockteHolder: SocketHolder?
    private val connectors: Array<Connectors>
    private val generatedSSLContext: IUserAgentSSLContext?

    enum class Connectors {
        HTTP_CONNECTOR, HTTPS_CONNECTOR, WS_CONNECTOR, WSS_CONNECTOR
    }

    companion object {
        const val HTTP_PORT = 8080
        const val HTTPS_PORT = 8443
        const val WSS_PORT = 9999
        const val WS_PORT = 9998
    }

    class Builder {
        var httpsPort: Int = HTTPS_PORT
        var httpPort: Int = HTTP_PORT
        var wssPort: Int = WSS_PORT
        var wsPort: Int = WS_PORT
        var hostName: String? = null
        var resourcePath: String? = null
        var rpcProcessor: RPCProcessor? = null
        var sockteHolder: SocketHolder? = null
        var connectors: Array<Connectors> = arrayOf()
        var generatedSSLContext: IUserAgentSSLContext? = null

        fun httpsPort(value : Int) = apply { httpsPort = value }

        fun httpPort(value : Int) = apply { httpPort = value }

        fun wssPort(value : Int) = apply { wssPort = value }

        fun wsPort(value : Int) = apply { wsPort = value }

        fun hostName(value : String?) = apply { hostName = value }

        fun resourcePath(value : String?) = apply { resourcePath = value }

        fun rpcProcessing(processor: RPCProcessor, holder: SocketHolder?) = apply {
            rpcProcessor = processor
            sockteHolder = holder
        }

        fun enableConnectors(value : Array<Connectors>) = apply { connectors = value }

        fun sslContext(value : IUserAgentSSLContext?) = apply { generatedSSLContext = value }

        fun build() = MiddlewareWebServer(this)
    }

    private var server: Server = Server()
        private set(value) {
            if (!value.isRunning) field = value
        }

    private var sslContextFactory: SslContextFactory? = null

    @Throws(Exception::class)
    private fun prepareServer() {
        configureSSLFactory()
        configureConnectors()
        configureHandlers()
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    private fun configureSSLFactory() {
        if (generatedSSLContext != null) {
            // Configuring SSL
            sslContextFactory = SslContextFactory.Server().apply {
                keyStoreType = "PKCS12"
                sslContext = generatedSSLContext.getInitializedSSLContext("MY_PASSWORD")
                setKeyStorePassword("MY_PASSWORD")
                setKeyManagerPassword("MY_PASSWORD")
            }
        }
    }

    private fun configureConnectors() {
        val enabledConnectors: MutableList<Connector> = ArrayList()

        // HTTP connector
        if (connectors.contains(Connectors.HTTP_CONNECTOR)) {
            val httpConnector = getServerConnector(httpPort)
            enabledConnectors.add(httpConnector)
        }

        // WS configuration
        if (connectors.contains(Connectors.WS_CONNECTOR)) {
            val wsConnector = getServerConnector(wsPort)
            enabledConnectors.add(wsConnector)
        }

        if (sslContextFactory != null) {
            // HTTPS configuration
            if (connectors.contains(Connectors.HTTPS_CONNECTOR)) {
                val httpsConnector = getSecureServerConnector(sslContextFactory, httpsPort)
                enabledConnectors.add(httpsConnector)
            }

            // WSS configuration
            if (connectors.contains(Connectors.WSS_CONNECTOR)) {
                val wssConnector = getSecureServerConnector(sslContextFactory, wssPort)
                enabledConnectors.add(wssConnector)
            }
        }

        // Setting HTTP, HTTPS, WS and WSS connectors
        if (enabledConnectors.isNotEmpty()) {
            server.connectors = enabledConnectors.toTypedArray()
        }
    }

    class MiddlewareWebSocketCreator(
            private val processor: RPCProcessor,
            private val sockteHolder: SocketHolder?
    ) : WebSocketCreator {
        override fun createWebSocket(req: ServletUpgradeRequest, resp: ServletUpgradeResponse): Any? {
            return MiddlewareWebSocket(processor, sockteHolder)
        }
    }

    private fun configureHandlers() {
        val handlerArray = ArrayList<Handler>()

        if(resourcePath != null) {
            handlerArray.add(ResourceHandler().apply {
                isDirectoriesListed = true
                welcomeFiles = arrayOf("index.html")
                baseResource = Resource.newResource(resourcePath)
            })
        }

        if (rpcProcessor != null) {
            handlerArray.add(object : WebSocketHandler() {
                override fun configure(factory: WebSocketServletFactory) {
                    factory.creator = MiddlewareWebSocketCreator(rpcProcessor, sockteHolder)
                }
            })
        }

        if (handlerArray.isNotEmpty()) {
            server.handler = HandlerList().apply {
                handlers = handlerArray.toTypedArray()
            }
        }
    }

    private fun getServerConnector(serverPort: Int): ServerConnector {
        return ServerConnector(server).apply {
            port = serverPort
            if (!hostName.isNullOrEmpty()) {
                host = hostName
            }
        }
    }

    private fun getSecureServerConnector(sslContextFactory: SslContextFactory?, serverPort: Int): ServerConnector {
        val config = HttpConfiguration().apply {
            addCustomizer(SecureRequestCustomizer())
            securePort = serverPort
        }

        // Configuring the secure connector
        return ServerConnector(server, SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString()), HttpConnectionFactory(config)).apply {
            port = serverPort
            if (!hostName.isNullOrEmpty()) {
                host = hostName
            }
        }
    }

    fun isRunning() = server.isRunning

    @Throws(MiddlewareWebServerError::class)
    fun start() {
        server.start()
    }

    @Throws(Exception::class)
    fun stop() {
        server.stop()
    }

    @Throws(Exception::class)
    override fun close() {
        stop()
    }

    init {
        httpsPort = builder.httpsPort
        httpPort = builder.httpPort
        wssPort = builder.wssPort
        wsPort = builder.wsPort
        hostName = builder.hostName
        resourcePath = builder.resourcePath
        rpcProcessor = builder.rpcProcessor
        sockteHolder = builder.sockteHolder
        connectors = builder.connectors
        generatedSSLContext = builder.generatedSSLContext

        prepareServer()
    }
}