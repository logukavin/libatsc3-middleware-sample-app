package org.ngbp.jsonrpc4jtestharness

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.ngbp.jsonrpc4jtestharness.http.servers.MiddlewareWebServer
import org.ngbp.jsonrpc4jtestharness.http.servers.MiddlewareWebServerError
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCManager
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCProcessor
import java.net.URI
import java.util.concurrent.CountDownLatch

class WebServerTests {
    private val lock: CountDownLatch = CountDownLatch(1)

    private var webServer: MiddlewareWebServer? = null
    private var rpcProcessor: RPCProcessor? = null
    private val HTTP_PORT = 8080
    private val HTTPS_PORT = 8443
    private val WS_PORT = 9998
    private val WSS_PORT = 9999
    private var serverUri: URI? = null

    @Before
    @Test
    @Throws(MiddlewareWebServerError::class)
    fun startServer() {
        //Server without ContentProviderServlet(applicationContext) and UserAgentSSLContext(applicationContext)

        val rpcManager = RPCManager()
        serverUri = URI(String.format("http://%s:%d/", "localHost", HTTP_PORT))
        rpcProcessor = RPCProcessor(rpcManager)
        webServer = MiddlewareWebServer.Builder()
                .hostName("localHost")
                .httpPort(HTTP_PORT)
                .httpsPort(HTTPS_PORT)
                .wsPort(WS_PORT)
                .wssPort(WSS_PORT)
                .enableConnectors(arrayOf(MiddlewareWebServer.Connectors.HTTPS_CONNECTOR, MiddlewareWebServer.Connectors.WSS_CONNECTOR))
                .addRPCProcessor(rpcProcessor!!)
                .build()
        webServer!!.start()
        Assert.assertEquals(true, webServer?.isRunning())
    }


    @Test
    @After
    fun stopServer() {
        webServer!!.stop()
        Assert.assertEquals(false, (webServer?.isRunning()))
    }
}