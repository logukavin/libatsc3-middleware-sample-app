package com.nextgenbroadcast.mobile.middleware.server

import com.nextgenbroadcast.mobile.core.cert.IUserAgentSSLContext
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.gateway.web.IWebGateway
import com.nextgenbroadcast.mobile.middleware.web.MiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.web.configureSSLFactory
import com.nextgenbroadcast.mobile.middleware.web.getSecureServerConnector
import com.nextgenbroadcast.mobile.middleware.web.getServerConnector
import com.nextgenbroadcast.mobile.middleware.ws.MiddlewareWebSocket
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.websocket.server.WebSocketHandler
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import java.util.*

class WebServerWithServlet {

    class Builder {
        private var httpsPort: Int? = null
        private var httpPort: Int? = null
        private var wssPort: Int? = null
        private var wsPort: Int? = null
        private var hostName: String? = null
        private var generatedSSLContext: IUserAgentSSLContext? = null
        private var rpcGateway: IRPCGateway? = null
        private var webGateway: IWebGateway? = null
        private var listOfServlet: List<ServletContainer>? = null
        fun httpsPort(value: Int) = apply { httpsPort = value }

        fun httpPort(value: Int) = apply { httpPort = value }

        fun wssPort(value: Int) = apply { wssPort = value }

        fun wsPort(value: Int) = apply { wsPort = value }

        fun hostName(value: String) = apply { hostName = value }

        fun sslContext(value: IUserAgentSSLContext) = apply { generatedSSLContext = value }

        fun rpcGateway(value: IRPCGateway) = apply { rpcGateway = value }

        fun webGateway(value: IWebGateway) = apply { webGateway = value }

        fun addServlets(value: List<ServletContainer>) = apply { listOfServlet = value }
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
            if (!listOfServlet.isNullOrEmpty()) {
                val context = ServletContextHandler(server, "/")
                server.handler = context
                listOfServlet?.forEach {
                    context.addServlet(ServletHolder(it.servlet), it.path)
                }

            }
            return MiddlewareWebServer(server, webGateway)
        }
    }
}
