package org.ngbp.jsonrpc4jtestharness.core.ws

import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketHolder @Inject constructor() {
    private val sessions = CopyOnWriteArrayList<MiddlewareWebSocket>()

    fun onOpen(socket: MiddlewareWebSocket) {
        sessions.add(socket)
    }

    fun onClose(socket: MiddlewareWebSocket) {
        sessions.remove(socket)
    }

    fun broadcastMessage(message: String) {
        sessions.forEach { socket ->
            socket.sendMessage(message)
        }
    }
}