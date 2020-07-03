package org.ngbp.jsonrpc4jtestharness.http.servers

import android.content.Context
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.server.WebSocketHandler
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import org.ngbp.jsonrpc4jtestharness.R
import org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWebSocket
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class MiddlewareWebServer(private val context: Context) : AutoCloseable {
    class MiddlewareSocketHandler : WebSocketHandler() {
        override fun configure(factory: WebSocketServletFactory) {
            factory.register(MiddlewareWebSocket::class.java)
        }
    }

    private lateinit var server: Server

    @Throws(Exception::class)
    fun runWebServer() {
        server = Server()
        val i = context.getResources().openRawResource(R.raw.mykey)
        val keystore = KeyStore.getInstance("PKCS12")
        try {
            keystore.load(i, "MY_PASSWORD".toCharArray())
        } finally {
            i.close()
        }
        val keyManagerFactory = KeyManagerFactory.getInstance("X509")
        keyManagerFactory.init(keystore, "MY_PASSWORD".toCharArray())
        val tmf = TrustManagerFactory.getInstance("X509")
        tmf.init(keystore)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, tmf.trustManagers, null)

        // Configuring SSL
        val sslContextFactory: SslContextFactory = SslContextFactory.Server()
        sslContextFactory.keyStoreType = "PKCS12"

        // Defining keystore path and passwords
        sslContextFactory.sslContext = sslContext
        sslContextFactory.setKeyStorePassword("MY_PASSWORD")
        sslContextFactory.setKeyManagerPassword("MY_PASSWORD")

        // HTTP connector
        val connector = ServerConnector(server)
        connector.port = 8081

        // HTTPS configuration
        val https = HttpConfiguration()
        https.addCustomizer(SecureRequestCustomizer())
        https.securePort = 8443
        // Configuring the https connector
        val sslConnector = ServerConnector(server, SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString()), HttpConnectionFactory(https))
        sslConnector.port = 8443

        // WSS configuration
        val wss = HttpConfiguration()
        https.addCustomizer(SecureRequestCustomizer())
        https.securePort = 9999
        // Configuring the wss connector
        val wssConnector = ServerConnector(server, SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString()), HttpConnectionFactory(wss))
        wssConnector.port = 9999

        // Setting HTTP and HTTPS and WSS connectors
        server.setConnectors(arrayOf<Connector?>(connector, sslConnector, wssConnector))
        val handler = ServletContextHandler(server, "/")
        val servlet = ContentProviderServlet(context)
        val holder = ServletHolder(servlet)
        handler.addServlet(holder, "/github")
        val ch = ContextHandler(server, "/echo")
        ch.handler = MiddlewareSocketHandler()
        val handlers = HandlerList()
        handlers.handlers = arrayOf<Handler?>(MiddlewareSocketHandler(), handler) //Added socket handler
        server.setHandler(handlers)
        try {
            server.start()
            server.join()
        } catch (t: Throwable) {
            t.printStackTrace(System.err)
        }
    }

    @Throws(Exception::class)
    override fun close() {
        stop()
    }

    @Throws(Exception::class)
    fun stop() {
        server.stop()
    }

    fun getServer(): Server? {
        return server
    }

}