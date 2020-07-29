package org.ngbp.jsonrpc4jtestharness.core.web

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.google.android.exoplayer2.util.Log
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.server.WebSocketHandler
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import org.ngbp.jsonrpc4jtestharness.core.cert.IUserAgentSSLContext
import org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWebSocket
import org.ngbp.jsonrpc4jtestharness.gateway.rpc.IRPCGateway
import org.ngbp.jsonrpc4jtestharness.gateway.web.IWebGateway
import org.ngbp.libatsc3.entities.app.Atsc3Application
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.*


class MiddlewareWebServer constructor(
        private val server: Server,
        private val webGateway: IWebGateway?
) : AutoCloseable, LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)

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
        server.start()
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
        //TODO: add applications to web server
    }

    class Builder {
        var httpsPort: Int? = null
        var httpPort: Int? = null
        var wssPort: Int? = null
        var wsPort: Int? = null
        var hostName: String? = null
        var generatedSSLContext: IUserAgentSSLContext? = null
        var rpcGateway: IRPCGateway? = null
        var webGateway: IWebGateway? = null

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
                //TODO: remove test data
                add(ResourceHandler().apply {
                    isDirectoriesListed = true
                    welcomeFiles = arrayOf("index.html")
                    baseResource = Resource.newResource("storage/emulated/0/Download/test")
                })

                rpcGateway?.let { rpcGateway ->
                    add(object : WebSocketHandler() {
                        override fun configure(factory: WebSocketServletFactory) {
                            factory.creator = WebSocketCreator { _, _ -> MiddlewareWebSocket(rpcGateway) }
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

@Throws(GeneralSecurityException::class, IOException::class)
private fun configureSSLFactory(generatedSSLContext: IUserAgentSSLContext): SslContextFactory {
    // Configuring SSL
    return SslContextFactory.Server().apply {
        keyStoreType = "PKCS12"
        //TODO: remove hardcoded password
        sslContext = generatedSSLContext.getInitializedSSLContext("MY_PASSWORD")
        setKeyStorePassword("MY_PASSWORD")
        setKeyManagerPassword("MY_PASSWORD")
    }
}

private fun getServerConnector(server: Server, serverHost: String, serverPort: Int): ServerConnector {
    return ServerConnector(server).apply {
        port = serverPort
        host = serverHost
    }
}

private fun getSecureServerConnector(server: Server, serverHost: String, serverPort: Int, sslContextFactory: SslContextFactory): ServerConnector {
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
