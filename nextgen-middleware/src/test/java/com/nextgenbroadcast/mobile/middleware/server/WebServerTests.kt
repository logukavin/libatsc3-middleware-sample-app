package com.nextgenbroadcast.mobile.middleware.server

import com.nextgenbroadcast.mobile.core.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.middleware.server.web.MiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.server.web.MiddlewareWebServerError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.modules.junit4.PowerMockRunner
import java.io.IOException
import java.security.KeyStore
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@RunWith(PowerMockRunner::class)
class WebServerTests : ServerTest() {
    @ExperimentalCoroutinesApi
    private val testDispatcher = TestCoroutineDispatcher()

    private lateinit var webServer: MiddlewareWebServer

    @ExperimentalCoroutinesApi
    @Before
    fun setup() {
        server.handler = ServletContextHandler(server, "/", ServletContextHandler.SESSIONS).apply {
            addServlet(ServletHolder(MiddlewareWebServerTestServlet()), "/index.html")
        }

        webServer = MiddlewareWebServer(server, webGateway = null).also {
            it.start()
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    @After
    fun tearDown() = testDispatcher.runBlockingTest {
        if (webServer.isRunning()) {
            webServer.stop()
            delay(500) // wait server get stopped
        }
        Assert.assertEquals(false, (webServer.isRunning()))
    }

    @ExperimentalCoroutinesApi
    @Test
    @Throws(MiddlewareWebServerError::class)
    fun startServer() = testDispatcher.runBlockingTest {
        delay(500) // wait server get started
        Assert.assertEquals(true, webServer.isRunning())
    }

    @Test
    fun makeHttpCall() {
        val client = OkHttpClient.Builder().connectionSpecs(listOf(ConnectionSpec.CLEARTEXT)).build()
        val request: Request = Request.Builder().url("http://localhost:8080/index.html").build()
        val response = client.newCall(request).execute()
        val serverMessage = response.body()?.string()
        val code = response.code()
        Assert.assertEquals(SERVER_MESSAGE, serverMessage)
        Assert.assertEquals(200, code)
        Assert.assertEquals(true, response.isSuccessful)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun makeHttpErrorCall() = testDispatcher.runBlockingTest {
        val client = OkHttpClient.Builder().connectionSpecs(listOf(ConnectionSpec.CLEARTEXT)).build()
        launch { delay(500) }
        val request: Request = Request.Builder().url("http://localhost:8080/index1.html").build()
        val response = client.newCall(request).execute()
        val serverMessage = response.body()?.string()
        val code = response.code()
        Assert.assertNotEquals(SERVER_MESSAGE, serverMessage)
        Assert.assertEquals(404, code)
        Assert.assertEquals(false, response.isSuccessful)
    }

    @Test
    fun makeHttpsCall() {
        val sslContext = UserAgentSSLContext(mockApplicationContext).getInitializedSSLContext("MY_PASSWORD")
        val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory, getTrustManager()).build()
        val request: Request = Request.Builder().url("https://localhost:8443/index.html").build()
        val response = client.newCall(request).execute()
        val serverMessage = response.body()?.string()
        val code = response.code()
        Assert.assertEquals(SERVER_MESSAGE, serverMessage)
        Assert.assertEquals(200, code)
    }

    @Test
    fun makeHttpsErrorCall() {
        val sslContext = UserAgentSSLContext(mockApplicationContext).getInitializedSSLContext("MY_PASSWORD")
        val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory, getTrustManager()).build()
        val request: Request = Request.Builder().url("https://localhost:8443/index1.html").build()
        val response = client.newCall(request).execute()
        val serverMessage = response.body()?.string()
        val code = response.code()
        Assert.assertNotEquals(SERVER_MESSAGE, serverMessage)
        Assert.assertEquals(404, code)
        Assert.assertEquals(false, response.isSuccessful)
    }

    private fun getTrustManager(): X509TrustManager {
        val trustManagerFactory: TrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)
        val trustManagers: Array<TrustManager> = trustManagerFactory.trustManagers
        check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) { "Unexpected default trust managers:${trustManagers.contentToString()}" }
        return trustManagers[0] as X509TrustManager
    }

    companion object {
        const val SERVER_MESSAGE = "Hello World"
    }
}

class MiddlewareWebServerTestServlet : HttpServlet() {

    @Throws(ServletException::class, IOException::class)
    override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
        with(response) {
            contentType = "text/html"
            status = HttpServletResponse.SC_OK
            writer.print(WebServerTests.SERVER_MESSAGE)
        }
    }
}