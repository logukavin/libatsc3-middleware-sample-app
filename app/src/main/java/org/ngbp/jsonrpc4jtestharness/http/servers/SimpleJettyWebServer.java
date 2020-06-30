package org.ngbp.jsonrpc4jtestharness.http.servers;

import android.content.Context;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.ngbp.jsonrpc4jtestharness.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SimpleJettyWebServer implements AutoCloseable {

    public SimpleJettyWebServer(String text, Context context) {
        SimpleJettyWebServer.text = text;
        this.context = context;
    }

    private Server server;
    private static String text;
    private Context context;

    public void startup() throws Exception {

        server = new Server();

        InputStream i = context.getResources().openRawResource(R.raw.jetty);

        KeyStore keystore = KeyStore.getInstance("bks");
        try {
            keystore.load(i, null);
        } finally {
            i.close();
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
        keyManagerFactory.init(keystore, null);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(keystore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), null);

        // HTTP connector
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);

        // HTTPS configuration
        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());
        https.setSecurePort(8443);

        // Configuring SSL
        SslContextFactory sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStoreType("bks");

        // Defining keystore path and passwords
//        sslContextFactory.setKeyStorePath("/raw/jetty.bks");
//        sslContextFactory.setKeyStore(keystore);
        sslContextFactory.setSslContext(sslContext);
        sslContextFactory.setKeyStorePassword("password");
        sslContextFactory.setKeyManagerPassword("password");

        // Configuring the connector
        ServerConnector sslConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https));
        sslConnector.setPort(8443);

        // Setting HTTP and HTTPS connectors
        server.setConnectors(new Connector[]{connector, sslConnector});

        ServletContextHandler handler = new ServletContextHandler(server, "/example");
        handler.addServlet(JsonRpcTestServlet.class, "/");

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

    public static class JsonRpcTestServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {

            PrintWriter out = response.getWriter();
            out.println(text);
        }
    }
}
