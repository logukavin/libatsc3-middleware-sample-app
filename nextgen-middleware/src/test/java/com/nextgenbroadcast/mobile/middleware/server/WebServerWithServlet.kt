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

class WebServerWithServlet(SSLContext: IUserAgentSSLContext) {
    fun getServer(): MiddlewareWebServer {
        return MiddlewareWebServer(server, webGateway)
    }
companion object{
    private const val HTTP_PORT = 8080
    private const val HTTPS_PORT = 8443
    private const val WS_PORT = 9998
    private const val WSS_PORT = 9999
    private const val HOST_NAME = "localhost"
}

    private var rpcGateway: IRPCGateway? = null
    private var webGateway: IWebGateway? = null
    private val server = Server()

    init {
        val connectorArray = HOST_NAME.let { hostName ->
            ArrayList<Connector>().apply {
                add(getServerConnector(server, hostName, HTTP_PORT))
                add(getServerConnector(server, hostName, WS_PORT))
                val sslContextFactory = configureSSLFactory(SSLContext)
                add(getSecureServerConnector(server, hostName, HTTPS_PORT, sslContextFactory))
                add(getSecureServerConnector(server, hostName, WSS_PORT, sslContextFactory))
            }
        }.toTypedArray()
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
        val listOfServlet: List<ServletContainer> = listOf(ServletContainer(MiddlewareWebServerTestServlet(), "/index.html"))
        val context = ServletContextHandler(server, "/")
        server.handler = context
        listOfServlet.forEach {
            context.addServlet(ServletHolder(it.servlet), it.path)
        }
    }
}