package org.ngbp.jsonrpc4jtestharness.core.ws;

import android.util.Log;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

public class EchoSocket extends WebSocketAdapter {
    private Session outbound;

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        super.onWebSocketBinary(payload, offset, len);
    }

    @Override
    public void onWebSocketText(String message) {
        super.onWebSocketText(message);
        Log.d("WSServer: ", "onWebSocketText: " + message);
        if ((outbound != null) && (outbound.isOpen())) {
            System.out.printf("Echoing back message [%s]%n", message);
            // echo the message back
            outbound.getRemote().sendString(message, null);
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
        Log.d("WSServer: ", "onWebSocketClose reason: " + reason + " , statusCode: " + statusCode);
        this.outbound = null;
    }

    @Override
    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session);
        Log.d("WSServer: ", "onWebSocketConnect: " + session.toString());
        this.outbound = session;
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause);
        Log.d("WSServer: ", "onWebSocketError: " + cause.getMessage());
        cause.printStackTrace(System.err);
    }

}

