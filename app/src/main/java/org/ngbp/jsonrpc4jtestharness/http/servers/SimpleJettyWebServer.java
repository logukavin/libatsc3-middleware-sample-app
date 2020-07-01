package org.ngbp.jsonrpc4jtestharness.http.servers;

import android.content.Context;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.ngbp.jsonrpc4jtestharness.R;

import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class SimpleJettyWebServer implements AutoCloseable {

    public SimpleJettyWebServer(Context context) {
        this.context = context;
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

        // Configuring SSL
        SslContextFactory sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStoreType("PKCS12");

        // Defining keystore path and passwords
//        sslContextFactory.setKeyStorePath("/raw/jetty.bks");
//        sslContextFactory.setKeyStore(keystore);
        sslContextFactory.setSslContext(sslContext);
        sslContextFactory.setKeyStorePassword("MY_PASSWORD");
        sslContextFactory.setKeyManagerPassword("MY_PASSWORD");

        // Configuring the connector
        ServerConnector sslConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString()), new HttpConnectionFactory(https));
        sslConnector.setPort(8443);

        // Setting HTTP and HTTPS connectors
        server.setConnectors(new Connector[]{connector, sslConnector});

        ServletContextHandler handler = new ServletContextHandler(server, "/");
        JsonRpcTestServlet servlet = new JsonRpcTestServlet(context);
        ServletHolder holder = new ServletHolder(servlet);
        handler.addServlet(holder, "/github");

        try {
            server.start();
//            server.join();
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
