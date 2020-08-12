package com.nextgenbroadcast.mobile.middleware.server

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nextgenbroadcast.mobile.core.cert.CertificateUtils
import com.nextgenbroadcast.mobile.core.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.web.MiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.web.MiddlewareWebServerError
import com.nextgenbroadcast.mobile.middleware.ws.MiddlewareWebSocket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import javax.net.ssl.KeyManagerFactory


@RunWith(PowerMockRunner::class)
@PrepareForTest(CertificateUtils::class, java.lang.String::class)
@PowerMockIgnore("javax.net.ssl.*", "javax.security.*")
class WebServerTests {
    @ExperimentalCoroutinesApi
    private val testDispatcher = TestCoroutineDispatcher()

    @Rule
    @ClassRule
    @JvmField
    var instantTaskExecutorRule = InstantTaskExecutorRule()

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

    private lateinit var webServer: MiddlewareWebServer
    private lateinit var webSocketClient: WebSocketClient

    private val classLoader = WebServerTests::class.java.javaClass.classLoader

    @ExperimentalCoroutinesApi
    @Before
    fun setup() = testDispatcher.runBlockingTest {
        PowerMockito.mockStatic(CertificateUtils::class.java)
        Mockito.`when`(CertificateUtils.KEY_MANAGER_ALGORITHM).thenReturn(KeyManagerFactory.getDefaultAlgorithm())
        if (classLoader != null)
            webServer = WebServerWithServlet(UserAgentSSLContext(classLoader.getResourceAsStream("mykey.p12"))).getServer().also {
                it.start()
            }
        webSocketClient = WebSocketClient()
    }

    @Test
    @After
    fun tearDown() {
        if (webSocketClient.isRunning) {
            webSocketClient.stop()
        }
        if (webServer.isRunning()) {
            webServer.stop()
        }
        Assert.assertEquals(false, (webServer.isRunning()))
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
        if (classLoader != null) {
            val client = OkHttpClient().newBuilder().sslSocketFactory(UserAgentSSLContext(classLoader.getResourceAsStream("mykey.p12")).getInitializedSSLContext("MY_PASSWORD").socketFactory).build()
            val request: Request = Request.Builder().url("https://localhost:8443/index.html").build()
            val response = client.newCall(request).execute()
            val serverMessage = response.body()?.string()
            val code = response.code()
            Assert.assertEquals(MiddlewareWebServerTestServlet.serverMessage, serverMessage)
            Assert.assertEquals(200, code)
            Assert.assertEquals(MiddlewareWebServerTestServlet.serverMessage, serverMessage)
        }
    }

    @Test
    fun makeHttpsErrorCall() {
        if (classLoader != null) {
            val client = OkHttpClient().newBuilder().sslSocketFactory(UserAgentSSLContext(classLoader.getResourceAsStream("mykey.p12")).getInitializedSSLContext("MY_PASSWORD").socketFactory).build()
            val request: Request = Request.Builder().url("https://localhost:8443/index1.html").build()
            val response = client.newCall(request).execute()
            val serverMessage = response.body()?.string()
            val code = response.code()
            Assert.assertNotEquals(MiddlewareWebServerTestServlet.serverMessage, serverMessage)
            Assert.assertEquals(404, code)
            Assert.assertEquals(false, response.isSuccessful)
        }
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