package org.ngbp.jsonrpc4jtestharness.core.ws

import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.client.WebSocketClient
import java.net.URI

class MiddlewareWebSocketClient {
    fun start() {
        val uri = URI.create("wss://localhost:9999/atscCmd")
        val sslContextFactory: SslContextFactory = SslContextFactory.Client()
        sslContextFactory.isTrustAll = true
        val http = HttpClient(sslContextFactory)
        val client = WebSocketClient(http)
        try {
            try {
                client.start()
                // The socket that receives events
                val socket = MiddlewareWebSocket()
                // Attempt Connect
                val fut = client.connect(socket, uri)
                // Wait for Connect
                val session = fut.get()
                // Send a message
                session.remote.sendString("Hello")
                // Close session
                session.close()
            } finally {
                client.stop()
            }
        } catch (t: Throwable) {
            t.printStackTrace(System.err)
        }
    }
}