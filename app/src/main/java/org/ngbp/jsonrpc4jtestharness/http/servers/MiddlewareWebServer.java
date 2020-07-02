package org.ngbp.jsonrpc4jtestharness.http.servers;

import android.content.Context;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.ngbp.jsonrpc4jtestharness.R;
import org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWebSocket;

import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class MiddlewareWebServer implements AutoCloseable {

    public MiddlewareWebServer(Context context) {
        this.context = context;
    }

    public static class MiddlewareSocketHandler extends WebSocketHandler {
        @Override
        public void configure(WebSocketServletFactory factory)
        {
            factory.register(MiddlewareWebSocket.class);
        }
    }

    private Server server;
    private Context context;

    public void runWebServer() throws Exception {

        server = new Server();

        InputStream i = context.getResources().openRawResource(R.raw.mykey);

        KeyStore keystore = KeyStore.getInstance("PKCS12");
        try {
            keystore.load(i, "MY_PASSWORD".toCharArray());
        } finally {
            i.close();
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
        keyManagerFactory.init(keystore, "MY_PASSWORD".toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(keystore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), null);

        // HTTP connector
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8081);

        // HTTPS configuration
        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());
        https.setSecurePort(8443);

        HttpConfiguration wss = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());
        https.setSecurePort(9999);

        // Configuring SSL
        SslContextFactory sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStoreType("PKCS12");

        // Defining keystore path and passwords
        sslContextFactory.setSslContext(sslContext);
        sslContextFactory.setKeyStorePassword("MY_PASSWORD");
        sslContextFactory.setKeyManagerPassword("MY_PASSWORD");

        // Configuring the connector
        ServerConnector sslConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString()), new HttpConnectionFactory(https));
        sslConnector.setPort(8443);

        ServerConnector wssConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString()), new HttpConnectionFactory(wss));
        sslConnector.setPort(9999);

        // Setting HTTP and HTTPS connectors
        server.setConnectors(new Connector[]{connector, sslConnector, wssConnector});

        ServletContextHandler handler = new ServletContextHandler(server, "/");
        JsonRpcTestServlet servlet = new JsonRpcTestServlet(context);
        ServletHolder holder = new ServletHolder(servlet);
        handler.addServlet(holder, "/github");

        ContextHandler ch = new ContextHandler(server,"/echo");
        ch.setHandler(new MiddlewareSocketHandler());

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {new MiddlewareSocketHandler(), handler}); //Added socket handler

        server.setHandler(handlers);

        try {
            server.start();
            server.join();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    @Override
    public void close() throws Exception {
        this.stop();
    }

    public void stop() throws Exception {
        server.stop();
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }
}
