package org.ngbp.jsonrpc4jtestharness.server

import org.eclipse.jetty.websocket.client.WebSocketClient
import org.junit.*
import org.mockito.Mock
import org.ngbp.jsonrpc4jtestharness.controller.IRPCController
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState

import org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWebSocket

import org.ngbp.jsonrpc4jtestharness.http.servers.MiddlewareWebServer
import org.ngbp.jsonrpc4jtestharness.http.servers.MiddlewareWebServerError

import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCProcessor
import java.net.URI

class WebServerTests {

    companion object {
        private const val HTTP_PORT = 8080
        private const val HTTPS_PORT = 8443
        private const val WS_PORT = 9998
        private const val WSS_PORT = 9999

        lateinit var rpcProcessor: RPCProcessor
        lateinit var webServer: MiddlewareWebServer
        lateinit var webSocketClient: WebSocketClient
        val wsUrl: String = String.format("ws://%s:%d/", "localhost", org.ngbp.jsonrpc4jtestharness.server.WebServerTests.Companion.WS_PORT)
        val httpUrl: String = String.format("http://%s:%d/", "localhost", org.ngbp.jsonrpc4jtestharness.server.WebServerTests.Companion.HTTP_PORT)

        @BeforeClass
        @JvmStatic
        fun setup() {
            rpcProcessor = RPCProcessor(object : IRPCController {
                override val language: String
                    get() = "test"
                override val queryServiceId: String?
                    get() = "test"
                override val mediaUrl: String?
                    get() = "test"
                override val playbackState: PlaybackState
                    get() = PlaybackState.IDLE

                override fun updateRMPPosition(scaleFactor: Double?, xPos: Double?, yPos: Double?) {
                    TODO("Not yet implemented")
                }

                override fun updateRMPState(state: PlaybackState) {
                    TODO("Not yet implemented")
                }
            })
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