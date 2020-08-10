package com.nextgenbroadcast.mobile.middleware.server

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LifecycleRegistry
import com.nextgenbroadcast.mobile.core.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.rpc.processor.RPCProcessor
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.Urls
import com.nextgenbroadcast.mobile.middleware.web.MiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.web.MiddlewareWebServerError
import com.nextgenbroadcast.mobile.middleware.ws.MiddlewareWebSocket
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.junit.*
import org.mockito.Mock


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
        @Rule
        @ClassRule
        @JvmField
        var instantTaskExecutorRule = InstantTaskExecutorRule()

        lateinit var rpcProcessor: RPCProcessor
        lateinit var webServer: MiddlewareWebServer
        lateinit var webSocketClient: WebSocketClient

        val classLoader = this.javaClass.classLoader
        @BeforeClass
        @JvmStatic
        fun setup() {
            System.setProperty("javax.net.ssl.trustStore", "NONE")
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
            })
            val servletList = listOf(ServletContainer(MiddlewareWebServerTestServlet(), "/index.html"))

            webServer = WebServerWithServlet.BuilderWithServlet()
                    .addServlets(servletList)
                    .hostName("localhost")
                    .httpPort(HTTP_PORT)
                    .httpsPort(HTTPS_PORT)
                    .wsPort(WS_PORT)
                    .wssPort(WSS_PORT)
                    .sslContext(UserAgentSSLContext(classLoader?.getResourceAsStream("mykey.p12")))
                    .build()
//            Whitebox.setInternalState(webServer, "lifecycleRegistry", lifecycleRegistry);
            webSocketClient = WebSocketClient()
            webServer.start()
        }
//
//        @AfterClass
//        @JvmStatic
//        fun tearDown() {
//            if (webSocketClient.isRunning) {
//                webSocketClient.stop()
//            }
//            if (webServer.isRunning()) {
//                webServer.stop()
//            }
//            Assert.assertEquals(false, (webServer.isRunning()))
//        }
    }

    @Test
    @Throws(MiddlewareWebServerError::class)
    fun startServer() {
        Assert.assertEquals(true, webServer.isRunning())
    }

    @Test
    fun makeHttpCall() {
        val client = OkHttpClient.Builder().connectionSpecs(listOf(ConnectionSpec.CLEARTEXT)).build()
        val request: Request = Request.Builder().url("http://localhost:8080/index.html").build()
        val response = client.newCall(request).execute()
        val serverMessage = response.body()?.string()
        val code = response.code()
        Assert.assertEquals(MiddlewareWebServerTestServlet.serverMessage, serverMessage)
        Assert.assertEquals(200, code)
        Assert.assertEquals(true, response.isSuccessful)
    }

    @Test
    fun makeHttpErrorCall() {
        val client = OkHttpClient.Builder().connectionSpecs(listOf(ConnectionSpec.CLEARTEXT)).build()
        val request: Request = Request.Builder().url("http://localhost:8080/index1.html").build()
        val response = client.newCall(request).execute()
        val serverMessage = response.body()?.string()
        val code = response.code()
        Assert.assertNotEquals(MiddlewareWebServerTestServlet.serverMessage, serverMessage)
        Assert.assertEquals(404, code)
        Assert.assertEquals(false, response.isSuccessful)
    }

    @Test
    fun makeHttpsCall() {
        val client = OkHttpClient().newBuilder().sslSocketFactory(UserAgentSSLContext(classLoader?.getResourceAsStream("mykey.p12")).getInitializedSSLContext("MY_PASSWORD").socketFactory).build()
        val request: Request = Request.Builder().url("https://localhost:8443/index.html").build()
        val response = client.newCall(request).execute()
        val serverMessage = response.body()?.string()
        val code = response.code()
        Assert.assertEquals(MiddlewareWebServerTestServlet.serverMessage, serverMessage)
        Assert.assertEquals(200, code)
        Assert.assertEquals(MiddlewareWebServerTestServlet.serverMessage, serverMessage)
    }

    @Test
    fun makeHttpsErrorCall() {
        val client = OkHttpClient().newBuilder().sslSocketFactory(UserAgentSSLContext(classLoader?.getResourceAsStream("mykey.p12")).getInitializedSSLContext("MY_PASSWORD").socketFactory).build()
        val request: Request = Request.Builder().url("https://localhost:8443/index1.html").build()
        val response = client.newCall(request).execute()
        val serverMessage = response.body()?.string()
        val code = response.code()
        Assert.assertNotEquals(MiddlewareWebServerTestServlet.serverMessage, serverMessage)
        Assert.assertEquals(404, code)
        Assert.assertEquals(false, response.isSuccessful)
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