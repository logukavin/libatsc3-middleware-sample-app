package com.nextgenbroadcast.mobile.middleware.server

import androidx.lifecycle.LifecycleRegistry
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
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.websocket.server.WebSocketHandler
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import java.util.ArrayList

class WebServerWithServlet(
        server: Server,
        webGateway: IWebGateway?
) : MiddlewareWebServer(server, webGateway) {

    class BuilderWithServlet : Builder() {
        var listOfServlet: List<ServletContainer>? = null
        fun addServlets(value: List<ServletContainer>) = apply { listOfServlet = value }
                override fun build(): WebServerWithServlet {
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
            if (!listOfServlet.isNullOrEmpty()) {
                val context = ServletContextHandler(server, "/")
                server.handler = context
                listOfServlet?.forEach {
                    context.addServlet(ServletHolder(it.servlet), it.path)
                }

            }
            return WebServerWithServlet(server, webGateway)
        }


    }
}
