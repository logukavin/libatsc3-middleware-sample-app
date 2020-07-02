package org.ngbp.jsonrpc4jtestharness.core.ws;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.ngbp.jsonrpc4jtestharness.jsonrpc2.IOnMessageListener;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MiddlewareWSServer extends WebSocketServer implements IOnRequest {
    private final IOnMessageListener listener;
    private String TAG = MiddlewareWSServer.class.getSimpleName();

    public MiddlewareWSServer(int port, IOnMessageListener listener) throws UnknownHostException {
        super(new InetSocketAddress(port));
        this.listener = listener;
    }

    public MiddlewareWSServer(String hostname, int port, IOnMessageListener listener) throws UnknownHostException {
        super(new InetSocketAddress(hostname, port));
        this.listener = listener;
    }

    public MiddlewareWSServer(InetSocketAddress address, IOnMessageListener listener) {
        super(address);
        this.listener = listener;
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
        if (listener != null)
            listener.onMessageReceiver(message);
        Log.i(TAG, "onMessage(String): " + message);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        broadcast(message.array());
        if (listener != null)
            listener.onMessageReceiver(message.array());
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

    @Override
    public void onRequest(String request) {
        broadcast(request);
        Log.i(TAG, "answer from RPC server " + request);
    }
}
