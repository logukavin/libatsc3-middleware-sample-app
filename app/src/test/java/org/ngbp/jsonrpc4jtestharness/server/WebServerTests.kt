package org.ngbp.jsonrpc4jtestharness.server

import org.eclipse.jetty.websocket.client.WebSocketClient
import org.junit.*

import org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWebSocket

import org.ngbp.jsonrpc4jtestharness.http.servers.MiddlewareWebServer
import org.ngbp.jsonrpc4jtestharness.http.servers.MiddlewareWebServerError
import org.ngbp.jsonrpc4jtestharness.rpc.manager.RPCManager
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCProcessor
import java.net.URI

class WebServerTests {

    companion object {
        private const val HTTP_PORT = 8080
        private const val HTTPS_PORT = 8443
        private const val WS_PORT = 9998
        private const val WSS_PORT = 9999

        lateinit var webServer: MiddlewareWebServer
        lateinit var webSocketClient: WebSocketClient
        val wsUrl: String = String.format("ws://%s:%d/", "localhost", org.ngbp.jsonrpc4jtestharness.server.WebServerTests.Companion.WS_PORT)
        val httpUrl: String = String.format("http://%s:%d/", "localhost", org.ngbp.jsonrpc4jtestharness.server.WebServerTests.Companion.HTTP_PORT)
        val rpcProcessor = RPCProcessor(RPCManager())

        @BeforeClass
        @JvmStatic
        fun setup() {
            //Server without ContentProviderServlet(applicationContext) and UserAgentSSLContext(applicationContext)
            webServer = MiddlewareWebServer.Builder()
                    .hostName("localhost")
                    .httpPort(HTTP_PORT)
                    .httpsPort(HTTPS_PORT)
                    .wsPort(WS_PORT)
                    .wssPort(WSS_PORT)
                    .enableConnectors(MiddlewareWebServer.Connectors.values())
                    .addRPCProcessor(rpcProcessor)
                    .build()

            webSocketClient = WebSocketClient()
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            if (webSocketClient.isRunning) {
                webSocketClient.stop()
            }
            if (webServer.isRunning()) {
                webServer.stop()
            }
            Assert.assertEquals(false, (webServer.isRunning()))
        }
    }


    @Test
    @Throws(MiddlewareWebServerError::class)
    fun startServer() {
        webServer?.start()
        Assert.assertEquals(true, webServer?.isRunning())
    }

    @Test
    fun testWSConnection() {
        webSocketClient.start()
        val session = webSocketClient.connect(MiddlewareWebSocket(rpcProcessor), URI(wsUrl)).get()

        Assert.assertTrue(session.isOpen)
    }
}