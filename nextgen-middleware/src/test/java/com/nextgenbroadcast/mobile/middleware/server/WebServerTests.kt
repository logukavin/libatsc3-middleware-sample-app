package com.nextgenbroadcast.mobile.middleware.server

import com.nextgenbroadcast.mobile.core.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.middleware.web.MiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.web.MiddlewareWebServerError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.junit.*
import org.junit.runner.RunWith
import org.powermock.modules.junit4.PowerMockRunner
import java.io.IOException
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

    @Test
    @After
    fun tearDown() {
        if (webServer.isRunning()) {
            webServer.stop()
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

    @Test
    fun makeHttpErrorCall() {
        val client = OkHttpClient.Builder().connectionSpecs(listOf(ConnectionSpec.CLEARTEXT)).build()
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
        val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory).build()
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
        val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory).build()
        val request: Request = Request.Builder().url("https://localhost:8443/index1.html").build()
        val response = client.newCall(request).execute()
        val serverMessage = response.body()?.string()
        val code = response.code()
        Assert.assertNotEquals(SERVER_MESSAGE, serverMessage)
        Assert.assertEquals(404, code)
        Assert.assertEquals(false, response.isSuccessful)
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