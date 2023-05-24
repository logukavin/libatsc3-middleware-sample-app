package com.nextgenbroadcast.mobile.middleware.server.ws

import android.util.Log
import com.nextgenbroadcast.mobile.middleware.rpc.processor.IRPCProcessor
import com.nextgenbroadcast.mobile.middleware.server.MiddlewareApplicationSession
import kotlinx.coroutines.delay
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter

class MiddlewareWebSocket(
        private val appSession: MiddlewareApplicationSession,
        private val rpcProcessor: IRPCProcessor
) : WebSocketAdapter() {

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

        appSession.finishSession()
    }

    override fun onWebSocketConnect(session: Session) {
        super.onWebSocketConnect(session)
        Log.d("WSServer: ", "onWebSocketConnect: " + session.localAddress)

        appSession.startSession(this)
    }

    override fun onWebSocketError(cause: Throwable) {
        super.onWebSocketError(cause)
        Log.d("WSServer: ", "onWebSocketError: " + cause.message)

        cause.printStackTrace(System.err)
    }

    fun sendMessage(message: String) {
        if (isConnected) {
            Log.d("WSServer: ", "--> onWebSocketText: $message")

            try {
                session.remote.sendString(message, null)
            } catch (ex: Exception) {
                Log.w("WSServer:sendMessage", "--> exception: $ex")

            }
        }
    }

    suspend fun disconnect() {
        if (!isConnected) return

        session.close()

        delay(500)

        if (isConnected) {
            disconnect()
        }
    }
}