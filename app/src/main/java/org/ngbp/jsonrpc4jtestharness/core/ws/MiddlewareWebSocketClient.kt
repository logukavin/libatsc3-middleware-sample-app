package org.ngbp.jsonrpc4jtestharness.core.ws

import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.ngbp.jsonrpc4jtestharness.rpc.processor.IRPCProcessor
import java.net.URI

class MiddlewareWebSocketClient(private var rpcProcessor: IRPCProcessor) {
    fun start() {
        val uri = URI.create("ws://localhost:9998/echo")
        val sslContextFactory: SslContextFactory = SslContextFactory.Client()
        sslContextFactory.isTrustAll = true
        val http = HttpClient(sslContextFactory)
        val client = WebSocketClient(http)
        try {
            try {
                client.start()
                // The socket that receives events
                val socket = MiddlewareWebSocket(rpcProcessor, null)
                // Attempt Connect
                val fut = client.connect(socket, uri)
                // Wait for Connect
                val session = fut.get()
                // Send a message
                session.remote.sendString("{\"jsonrpc\":\"2.0\",\"method\":\"org.atsc.subscribe\",\"params\":{\"msgType\":[\"All\"]},\"id\":1}")
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