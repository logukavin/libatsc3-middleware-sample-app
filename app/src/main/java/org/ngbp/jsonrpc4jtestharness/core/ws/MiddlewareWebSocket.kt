package org.ngbp.jsonrpc4jtestharness.core.ws

import android.util.Log
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.ngbp.jsonrpc4jtestharness.rpc.processor.IRPCProcessor

class MiddlewareWebSocket(
        private val rpcProcessor: IRPCProcessor
) : WebSocketAdapter() {

    private var outbound: Session? = null

    override fun onWebSocketBinary(payload: ByteArray?, offset: Int, len: Int) {
        super.onWebSocketBinary(payload, offset, len)
    }

    override fun onWebSocketText(message: String?) {
        super.onWebSocketText(message)
        Log.d("WSServer: ", "onWebSocketText: $message")

        if (message != null) {
            val response = rpcProcessor.processRequest(message)

            outbound?.let { session ->
                if (session.isOpen) {
                    session.remote.sendString(response, null)
                }
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
        Log.d("WSServer: ", "onWebSocketConnect: " + session.localAddress)

        outbound = session
    }

    override fun onWebSocketError(cause: Throwable) {
        super.onWebSocketError(cause)
        Log.d("WSServer: ", "onWebSocketError: " + cause.message)

        cause.printStackTrace(System.err)
    }
}