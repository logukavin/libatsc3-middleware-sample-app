package org.ngbp.jsonrpc4jtestharness.core.ws;


import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.CustomSSLWebSocketServerFactory;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;


import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

public class MiddlewareWSSServer extends WebSocketServer {
    private static final String TAG = MiddlewareWSSServer.class.getSimpleName();

    private SSLContext sslContext;

    public MiddlewareWSSServer(InputStream keyStoreStream, int port) throws UnknownHostException, NoSuchAlgorithmException {
        this(keyStoreStream, new InetSocketAddress(port));
    }

    public MiddlewareWSSServer(InputStream keyStoreStream, String hostname, int port) throws UnknownHostException, NoSuchAlgorithmException {
        this(keyStoreStream, new InetSocketAddress(hostname, port));
    }

    public MiddlewareWSSServer(InputStream keyStoreStream, InetSocketAddress address) throws NoSuchAlgorithmException {
        super(address);
        sslContext = getSSLContextFromKeyStore(keyStoreStream);
        setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.send("test onOpen");
        broadcast("new connection: " + handshake.getResourceDescriptor());
        Log.i(TAG, "new connection: " + handshake.getResourceDescriptor());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        broadcast("connection closed with: " + conn);
        Log.i(TAG, "connection closed with: " + conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        broadcast(message);
        if (message.equals("hop_open")) {
            broadcast("hop_hop_hop");
        }
        Log.i(TAG, "onMessage(String): " + message);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        broadcast(message.array());
        Log.i(TAG, "onMessage(BB): " + Arrays.toString(message.array()));
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        Log.i(TAG, "Server started");
        setConnectionLostTimeout(100);
    }


    private SSLContext getSSLContextFromKeyStore(InputStream keyStoreStream) {
        // load up the key store
        String storePassword = "password";
        String keyPassword = "password";

        SSLContext sslContext;
        try {
            KeyStore keystore = KeyStore.getInstance("BKS");
            try {
                keystore.load(keyStoreStream, storePassword.toCharArray());
            } finally {
                keyStoreStream.close();
            }
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
            keyManagerFactory.init(keystore, keyPassword.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(keystore);

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), null);


            SSLEngine engine = sslContext.createSSLEngine();
            List<String> ciphers = new ArrayList<String>( Arrays.asList(engine.getEnabledCipherSuites()));
            ciphers.remove("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
            List<String> protocols = new ArrayList<String>( Arrays.asList(engine.getEnabledProtocols()));
            protocols.remove("SSLv3");

            CustomSSLWebSocketServerFactory factory = new CustomSSLWebSocketServerFactory(sslContext, protocols.toArray(new String[]{}), ciphers.toArray(new String[]{}));
            setWebSocketFactory(factory);
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException e) {
            throw new IllegalArgumentException();
        }
        return sslContext;
    }
}
