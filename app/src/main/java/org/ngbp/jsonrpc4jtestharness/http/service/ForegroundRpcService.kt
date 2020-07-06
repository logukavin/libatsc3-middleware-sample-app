package org.ngbp.jsonrpc4jtestharness.http.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWebSocket
import org.ngbp.jsonrpc4jtestharness.core.ws.UserAgentSSLContext
import org.ngbp.jsonrpc4jtestharness.http.servers.ContentProviderServlet
import org.ngbp.jsonrpc4jtestharness.http.servers.MiddlewareWebServer
import java.util.*

class ForegroundRpcService : Service() {
    private lateinit var wakeLock: WakeLock
    private lateinit var notificationHelper: NotificationHelper

    private var isServiceStarted: Boolean = false
    private var webServer: MiddlewareWebServer? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        notificationHelper = NotificationHelper(this)

        val message = intent.getStringExtra("inputExtra")
        val action = intent.getAction()
        if (action != null) {
            when (action) {
                ACTION_START -> startService(message)
                ACTION_STOP -> stopService()
                else -> {
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startService(message: String?) {
        if (isServiceStarted) return
        isServiceStarted = true
        wakeLock = (Objects.requireNonNull(getSystemService(Context.POWER_SERVICE)) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ForegroundRpcService::lock").apply {
            acquire()
        }
        startForeground(1, notificationHelper.createNotification(message))
        startWebServer()
    }

    private fun startWebServer() {
        val serverThread: Thread = object : Thread() {
            override fun run() {
                webServer = MiddlewareWebServer.Builder()
                        .hostName("localHost")
                        .httpPort(8080)
                        .httpsPort(8443)
                        .wsPort(9998)
                        .wssPort(9999)
                        .addWebSocket(MiddlewareWebSocket())
                        .addServlet(ContentProviderServlet(applicationContext))
                        .sslContext(UserAgentSSLContext(applicationContext))
                        .enableConnectors(arrayOf(MiddlewareWebServer.Connectors.HTTPS_CONNECTOR, MiddlewareWebServer.Connectors.WSS_CONNECTOR))
                        .build()
                webServer?.start()
            }
        }
        serverThread.start()
    }

    private fun stopService() {
        if (isServiceStarted) {
            wakeLock.release()

            webServer?.let { server ->
                if (server.isRunning()) {
                    try {
                        server.stop()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            stopForeground(true)
            stopSelf()
        }
        isServiceStarted = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        val ACTION_START: String? = "START"
        val ACTION_STOP: String? = "STOP"
    }
}