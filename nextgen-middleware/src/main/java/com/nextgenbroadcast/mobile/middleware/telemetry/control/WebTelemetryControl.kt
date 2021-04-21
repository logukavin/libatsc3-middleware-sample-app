package com.nextgenbroadcast.mobile.middleware.telemetry.control

import com.nextgenbroadcast.mobile.middleware.gateway.web.ConnectionType
import com.nextgenbroadcast.mobile.middleware.server.web.IMiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryControl
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect

class WebTelemetryControl(
        private val webServer: IMiddlewareWebServer
) : ITelemetryControl {

    override suspend fun subscribe(commandFlow: MutableSharedFlow<TelemetryControl>) {
        webServer.addConnection(CONNECTION_TYPE, CONNECTION_HOST, CONNECTION_PORT)

        callbackFlow {
            webServer.addHandler(CONNECTION_PATH) { req, resp ->
                val cmdAction = if (req.pathInfo.length > 1) req.pathInfo.substring(1) else req.pathInfo
                val cmdArgs = req.parameterMap.mapValues { (_, value) ->
                    value.joinToString(",")
                }

                sendBlocking(TelemetryControl().apply {
                    action = cmdAction
                    arguments = cmdArgs
                })

                resp.writer.apply {
                    println("Receive command: $cmdAction, params: $cmdArgs")
                }
            }

            awaitClose {
                webServer.removeHandler(CONNECTION_PATH)
            }
        }.buffer(Channel.CONFLATED) // To avoid blocking
                .collect { data ->
                    commandFlow.emit(data)
                }
    }

    companion object {
        private val CONNECTION_TYPE = ConnectionType.HTTP
        private const val CONNECTION_HOST = "0.0.0.0"
        private const val CONNECTION_PORT = 8080
        private const val CONNECTION_PATH = "command"
    }
}