package org.ngbp.jsonrpc4jtestharness.core.ws;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.net.URI;
import java.util.concurrent.Future;

public class MiddlewareWebSocketClient {

    public void start() {
        URI uri = URI.create("wss://localhost:9999/atscCmd");

        SslContextFactory sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setTrustAll(true);
        HttpClient http = new HttpClient(sslContextFactory);

        WebSocketClient client = new WebSocketClient(http);
        try {
            try {
                client.start();
                // The socket that receives events
                MiddlewareWebSocket socket = new MiddlewareWebSocket();
                // Attempt Connect
                Future<Session> fut = client.connect(socket,uri);
                // Wait for Connect
                Session session = fut.get();
                // Send a message
                session.getRemote().sendString("Hello");
                // Close session
                session.close();
            } finally {
                client.stop();
            }
        }
        catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }
}
