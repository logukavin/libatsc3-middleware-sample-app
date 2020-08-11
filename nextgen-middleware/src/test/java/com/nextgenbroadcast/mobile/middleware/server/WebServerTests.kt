package com.nextgenbroadcast.mobile.middleware.server

import org.eclipse.jetty.websocket.client.WebSocketClient
import org.junit.*
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.core.model.PlaybackState

import com.nextgenbroadcast.mobile.middleware.ws.MiddlewareWebSocket

import com.nextgenbroadcast.mobile.middleware.web.MiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.web.MiddlewareWebServerError
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType

import com.nextgenbroadcast.mobile.middleware.rpc.processor.RPCProcessor
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.Urls

class WebServerTests {

    abstract class RPCGatewayAdapter : IRPCGateway {
        override fun updateRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double) {
            TODO("Not yet implemented")
        }

        override fun requestMediaPlay(mediaUrl: String?, delay: Long) {
            TODO("Not yet implemented")
        }

        override fun requestMediaStop(delay: Long) {
            TODO("Not yet implemented")
        }

        override fun subscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType> {
            TODO("Not yet implemented")
        }

        override fun unsubscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType> {
            TODO("Not yet implemented")
        }

        override fun sendNotification(message: String) {
            TODO("Not yet implemented")
        }

        override fun onSocketOpened(socket: MiddlewareWebSocket) {
            TODO("Not yet implemented")
        }

        override fun onSocketClosed(socket: MiddlewareWebSocket) {
            TODO("Not yet implemented")
        }
    }

    companion object {
        private const val HTTP_PORT = 8080
        private const val HTTPS_PORT = 8443
        private const val WS_PORT = 9998
        private const val WSS_PORT = 9999

        lateinit var rpcProcessor: RPCProcessor
        lateinit var webServer: MiddlewareWebServer
        lateinit var webSocketClient: WebSocketClient
        val wsUrl: String = String.format("ws://%s:%d/", "localhost", WS_PORT)
        val httpUrl: String = String.format("http://%s:%d/", "localhost", HTTP_PORT)

        @BeforeClass
        @JvmStatic
        fun setup() {
            rpcProcessor = RPCProcessor(object : RPCGatewayAdapter() {
                override val language: String
                    get() = "test"
                override val queryServiceId: String?
                    get() = "test"
                override val mediaUrl: String?
                    get() = "test"
                override val playbackState: PlaybackState
                    get() = PlaybackState.IDLE
                override val serviceGuideUrls: List<Urls>
                    get() = emptyList()

                override fun getDeviceId(): String {
                    TODO("Not yet implemented")
                }

                override fun getAdvertisingId(): String {
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
        webServer.start()
        Assert.assertEquals(true, webServer.isRunning())
    }

    @Test
    fun testWSConnection() {
// TODO: Caused by: org.eclipse.jetty.websocket.api.UpgradeException at WebSocketUpgradeRequest.java:532

//        webSocketClient.start()
//        val session = webSocketClient.connect(MiddlewareWebSocket(object : RPCGatewayAdapter() {
//            override val language: String
//                get() = "test"
//            override val queryServiceId: String?
//                get() = "test"
//            override val mediaUrl: String?
//                get() = "test"
//            override val playbackState: PlaybackState
//                get() = PlaybackState.IDLE
//            override val serviceGuideUrls: List<Urls>
//                get() = emptyList()
//        }), URI(wsUrl)).get()
//
//        Assert.assertTrue(session.isOpen)
    }
}