package org.ngbp.jsonrpc4jtestharness.core.ws

import android.util.Log
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.ngbp.jsonrpc4jtestharness.rpc.processor.IRPCProcessor

class MiddlewareWebSocket(
        private val rpcProcessor: IRPCProcessor,
        private val holder: SocketHolder?
) : WebSocketAdapter() {

    private var outbound: Session? = null

    override fun onWebSocketText(message: String?) {
        super.onWebSocketText(message)
        Log.d("WSServer: ", "<-- onWebSocketText: $message")

        if (message != null) {
            val response = rpcProcessor.processRequest(message)

            sendMessage(response)
        }
    }

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        super.onWebSocketClose(statusCode, reason)
        Log.d("WSServer: ", "onWebSocketClose reason: $reason , statusCode: $statusCode")

        holder?.onClose(this)

        outbound = null
    }

    override fun onWebSocketConnect(session: Session) {
        super.onWebSocketConnect(session)
        Log.d("WSServer: ", "onWebSocketConnect: " + session.localAddress)

        outbound = session

        holder?.onOpen(this)
    }

    override fun onWebSocketError(cause: Throwable) {
        super.onWebSocketError(cause)
        Log.d("WSServer: ", "onWebSocketError: " + cause.message)

        cause.printStackTrace(System.err)
    }

    fun sendMessage(message: String) {
        if (outbound?.isOpen == true) {
            Log.d("WSServer: ", "--> onWebSocketText: $message")

            session.remote.sendString(message, null)
        }
    }
}