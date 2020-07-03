package org.ngbp.jsonrpc4jtestharness.core.ws

import android.util.Log
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter

class MiddlewareWebSocket : WebSocketAdapter() {
    private var outbound: Session? = null
    override fun onWebSocketBinary(payload: ByteArray?, offset: Int, len: Int) {
        super.onWebSocketBinary(payload, offset, len)
    }

    override fun onWebSocketText(message: String?) {
        super.onWebSocketText(message)
        Log.d("WSServer: ", "onWebSocketText: $message")
        outbound?.let { session ->
            if (session.isOpen()) {
                System.out.printf("Echoing back message [%s]%n", message)
                // echo the message back
                session.getRemote().sendString(message, null)
            }
        }
    }

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        super.onWebSocketClose(statusCode, reason)
        Log.d("WSServer: ", "onWebSocketClose reason: $reason , statusCode: $statusCode")
        outbound = null
    }

    override fun onWebSocketConnect(session: Session) {
        super.onWebSocketConnect(session)
        Log.d("WSServer: ", "onWebSocketConnect: " + session.getLocalAddress())
        outbound = session
    }

    override fun onWebSocketError(cause: Throwable) {
        super.onWebSocketError(cause)
        Log.d("WSServer: ", "onWebSocketError: " + cause.message)
        cause.printStackTrace(System.err)
    }
}