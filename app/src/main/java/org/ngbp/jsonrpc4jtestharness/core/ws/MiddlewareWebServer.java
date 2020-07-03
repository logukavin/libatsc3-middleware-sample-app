package org.ngbp.jsonrpc4jtestharness.core.ws;

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
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServlet;

public class MiddlewareWebServer implements AutoCloseable {

    public enum Connectors {
        HTTP_CONNECTOR, HTTPS_CONNECTOR, WS_CONNECTOR, WSS_CONNECTOR
    }

    private final int httpsPort;
    private final int httpPort;
    private final int wssPort;
    private final int wsPort;
    private String hostName;
    private HttpServlet servlet;
    private WebSocketAdapter webSocket;
    private Connectors[] connectors;
    private IGenerateSSLContext generatedSSLContext;

    public static class Builder {

        private int httpsPort;
        private int httpPort;
        private int wssPort;
        private int wsPort;
        private String hostName;
        private HttpServlet servlet;
        private WebSocketAdapter webSocket;
        private Connectors[] connectors;
        private IGenerateSSLContext generatedSSLContext;

        public Builder() {
            httpsPort = 0;
            httpPort = 0;
            wssPort = 0;
            wsPort = 0;
            hostName = null;
            servlet = null;
            webSocket = null;
            connectors = new Connectors[]{};
            generatedSSLContext = null;
        }

        public Builder httpsPort(int val) {
            httpsPort = val;
            return this;
        }

        public Builder httpPort(int val) {
            httpPort = val;
            return this;
        }

        public Builder wssPort(int val) {
            wssPort = val;
            return this;
        }

        public Builder wsPort(int val) {
            wsPort = val;
            return this;
        }

        public Builder hostName(String val) {
            hostName = val;
            return this;
        }

        public Builder addServlet(HttpServlet val) {
            servlet = val;
            return this;
        }

        public Builder addWebSocket(WebSocketAdapter val) {
            webSocket = val;
            return this;
        }

        public Builder enableConnectors(Connectors[] val) {
            connectors = val;
            return this;
        }

        public Builder sslContext(IGenerateSSLContext val) {
            generatedSSLContext = val;
            return this;
        }

        public MiddlewareWebServer build() {
            return new MiddlewareWebServer(this);
        }
    }

    private MiddlewareWebServer(Builder builder) {
        httpsPort = builder.httpsPort;
        httpPort = builder.httpPort;
        wssPort = builder.wssPort;
        wsPort = builder.wsPort;
        hostName = builder.hostName;
        servlet = builder.servlet;
        webSocket = builder.webSocket;
        connectors = builder.connectors;
        generatedSSLContext = builder.generatedSSLContext;

        try {
            prepareServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Server server;
    private SslContextFactory sslContextFactory;

    private void prepareServer() throws Exception {
        server = new Server();

        configureSSLFactory();
        configureConnectors();
        configureHandlers();
    }

    private void configureSSLFactory() throws GeneralSecurityException, IOException {
        if (generatedSSLContext != null) {
            // Configuring SSL
            sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStoreType("PKCS12");

            // Defining keystore path and passwords
            sslContextFactory.setSslContext(generatedSSLContext.getInitializedSSLContext( "MY_PASSWORD"));
            sslContextFactory.setKeyStorePassword("MY_PASSWORD");
            sslContextFactory.setKeyManagerPassword("MY_PASSWORD");
        }
    }

    private void configureConnectors() {
        List<Connector> enabledConnectors = new ArrayList<>();

        // HTTP connector
        if (Arrays.asList(connectors).contains(Connectors.HTTP_CONNECTOR)) {
            ServerConnector httpConnector = getServerConnector(httpPort);
            enabledConnectors.add(httpConnector);
        }

        // WS configuration
        if (Arrays.asList(connectors).contains(Connectors.WS_CONNECTOR)) {
            ServerConnector wsConnector = getServerConnector(wsPort);
            enabledConnectors.add(wsConnector);
        }

        if (sslContextFactory != null) {
            // HTTPS configuration
            if (Arrays.asList(connectors).contains(Connectors.HTTPS_CONNECTOR)) {
                ServerConnector httpsConnector = getSecureServerConnector(sslContextFactory, httpsPort);
                enabledConnectors.add(httpsConnector);
            }

            // WSS configuration
            if (Arrays.asList(connectors).contains(Connectors.WSS_CONNECTOR)) {
                ServerConnector wssConnector = getSecureServerConnector(sslContextFactory, wssPort);
                enabledConnectors.add(wssConnector);
            }
        }

        // Setting HTTP, HTTPS, WS and WSS connectors
        if (!enabledConnectors.isEmpty()) {
            server.setConnectors(enabledConnectors.toArray(new Connector[]{}));
        }
    }

    private void configureHandlers() {
        List<Handler> handlerArray = new ArrayList<>();

        if (webSocket != null) {
            WebSocketHandler webSocketHandler = new WebSocketHandler() {
                @Override
                public void configure(WebSocketServletFactory factory) {
                    factory.register(webSocket.getClass());
                }
            };
            ContextHandler contextHandler = new ContextHandler(server,"/atscCmd");
            contextHandler.setHandler(webSocketHandler);

            handlerArray.add(contextHandler);
        }

        if (servlet != null) {
            ServletContextHandler servletHandler = new ServletContextHandler(server, "/");
            ServletHolder servletHolder = new ServletHolder(servlet);
            servletHandler.addServlet(servletHolder, "/github");

            handlerArray.add(servletHandler);
        }

        if (!handlerArray.isEmpty()) {
            HandlerList handlers = new HandlerList();
            handlers.setHandlers(handlerArray.toArray(new Handler[]{}));
            server.setHandler(handlers);
        }
    }

    private ServerConnector getServerConnector(int port) {
        ServerConnector connector = new ServerConnector(server);
        if (port != 0) {
            connector.setPort(port);
        }
        if (!hostName.isEmpty()) {
            connector.setHost(hostName);
        }
        return connector;
    }

    private ServerConnector getSecureServerConnector(SslContextFactory sslContextFactory, int securePort) {
        HttpConfiguration config = new HttpConfiguration();
        config.addCustomizer(new SecureRequestCustomizer());
        if (securePort != 0) {
            config.setSecurePort(securePort);
        }
        // Configuring the secure connector
        ServerConnector connector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString()), new HttpConnectionFactory(config));
        if (securePort != 0) {
            connector.setPort(securePort);
        }
        if (!hostName.isEmpty()) {
            connector.setHost(hostName);
        }
        return connector;
    }

    public void  start() {
        try {
            server.start();
            server.join();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    public void stop() throws Exception {
        server.stop();
    }

    public Server getServer() {
        return server;
    }

    @Override
    public void close() throws Exception {
        this.stop();
    }
}

