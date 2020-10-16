package com.nextgenbroadcast.mobile.middleware.server

import com.nextgenbroadcast.mobile.core.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.Urls
import com.nextgenbroadcast.mobile.middleware.server.web.MiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.server.web.configureSSLFactory
import com.nextgenbroadcast.mobile.middleware.server.ws.MiddlewareWebSocket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.eclipse.jetty.websocket.server.WebSocketHandler
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.modules.junit4.PowerMockRunner
import java.net.URI

@RunWith(PowerMockRunner::class)
class SocketServerTest : ServerTest() {
    private lateinit var rpcGateway: IRPCGateway
    private lateinit var webServer: MiddlewareWebServer
    private lateinit var webSocketClient: WebSocketClient

    @ExperimentalCoroutinesApi
    @Before
    fun setup() {
        rpcGateway = object : RPCGatewayAdapter() {
            override val deviceId: String
                get() = "e0351bc7-3008-484e-8940-a24cede0c9cf"
            override val advertisingId: String
                get() = "2d48ed24-fa78-4aeb-bd9b-0e1aade2b575"
            override val language: String
                get() = "test"
            override val queryServiceId: String?
                get() = "test"
            override val mediaUrl: String?
                get() = "test"
            override val playbackState: PlaybackState
                get() = PlaybackState.IDLE
            override val serviceGuideUrls: List<Urls>
                get() = emptyList()
        }

        server.handler = object : WebSocketHandler() {
            override fun configure(factory: WebSocketServletFactory) {
                factory.creator = WebSocketCreator { _, _ -> MiddlewareWebSocket(rpcGateway) }
            }
        }

        webServer = MiddlewareWebServer(server, webGateway = null).also {
            it.start(null)
        }
        webSocketClient = WebSocketClient(HttpClient(configureSSLFactory(UserAgentSSLContext(mockApplicationContext))))
    }

    @Test
    @After
    fun tearDown() {
        if (webSocketClient.isRunning) {
            webSocketClient.stop()
        }
        if (webServer.isRunning()) {
            webServer.stop()
        }
        Assert.assertEquals(false, (webServer.isRunning()))
    }

    @Test
    fun testWSConnection() {
        webSocketClient.start()
        val session = webSocketClient.connect(WebSocketAdapter(), URI("ws://localhost:$WS_PORT/")).get()

        Assert.assertTrue(session.isOpen)
    }

    @Test
    fun testWSSConnection() {
        webSocketClient.start()
        val session = webSocketClient.connect(WebSocketAdapter(), URI("wss://localhost:$WSS_PORT/")).get()

        Assert.assertTrue(session.isOpen)
    }
}

abstract class RPCGatewayAdapter : IRPCGateway {
    override fun updateRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double) {
        TODO("Not yet implemented")
    }

    override fun requestMediaPlay(mediaUrl: String?, delay: Long) {
        TODO("Not yet implemented")
    }

    override fun requestMediaStop(delay: Long) {
        TODO("Not yet implemented")
    }

    override fun subscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType> {
        TODO("Not yet implemented")
    }

    override fun unsubscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType> {
        TODO("Not yet implemented")
    }

    override fun sendNotification(message: String) {
        TODO("Not yet implemented")
    }

    override fun onSocketOpened(socket: MiddlewareWebSocket) {
        Assert.assertNotNull(socket)
    }

    override fun onSocketClosed(socket: MiddlewareWebSocket) {
        Assert.assertNotNull(socket)
    }
}