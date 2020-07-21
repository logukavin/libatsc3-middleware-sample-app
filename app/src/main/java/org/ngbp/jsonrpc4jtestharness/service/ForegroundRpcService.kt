package org.ngbp.jsonrpc4jtestharness.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import androidx.lifecycle.LifecycleService
import dagger.android.AndroidInjection
import org.ngbp.jsonrpc4jtestharness.controller.IMediaPlayerController
import org.ngbp.jsonrpc4jtestharness.controller.IReceiverController
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.core.ws.UserAgentSSLContext
import org.ngbp.jsonrpc4jtestharness.http.servers.MiddlewareWebServer
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCProcessor
import java.util.*
import javax.inject.Inject

class ForegroundRpcService : LifecycleService() {
    @Inject
    lateinit var rpcProcessor: RPCProcessor
    @Inject
    lateinit var receiverController: IReceiverController
    @Inject
    lateinit var mediaController: IMediaPlayerController

    private lateinit var wakeLock: WakeLock
    private lateinit var notificationHelper: NotificationHelper

    private var isServiceStarted: Boolean = false
    private var webServer: MiddlewareWebServer? = null

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()

        notificationHelper = NotificationHelper(this, NOTIFICATION_CHANNEL_ID).also {
            it.createNotificationChannel("Foreground Rpc Service Channel")
        }

        startForeground(NOTIFICATION_ID, createNotification(getServiceName()))

        receiverController.selectedService.observe(this, androidx.lifecycle.Observer {
            updateNotification()
        })
        mediaController.rmpState.observe(this, androidx.lifecycle.Observer {
            updateNotification()
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val action = intent?.action
        if (action != null) {
            when (action) {
                ACTION_START -> {
                    val message = intent.getStringExtra("inputExtra") ?: ""
                    startService(message)
                }
                ACTION_STOP -> stopService()
                else -> {
                }
            }
        }

        return START_NOT_STICKY
    }
    
    private fun getServiceName() = receiverController.selectedService.value?.shortName ?: "---"

    private fun startService(message: String) {
        if (isServiceStarted) return
        isServiceStarted = true
        wakeLock = (Objects.requireNonNull(getSystemService(Context.POWER_SERVICE)) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ForegroundRpcService::lock").apply {
            acquire()
        }
        startForeground(NOTIFICATION_ID, createNotification(getServiceName(), message))
        startWebServer()
    }

    private fun createNotification(title: String, message: String = "", playbackState: PlaybackState = PlaybackState.IDLE): Notification {
        return notificationHelper.createMediaNotification(title, message, playbackState)
    }

    private fun updateNotification() {
        val serviceName = receiverController.selectedService.value?.shortName ?: ""
        notificationHelper.notify(NOTIFICATION_ID, createNotification(serviceName, "", mediaController.rmpState.value ?: PlaybackState.IDLE))
    }

    private fun startWebServer() {
        val serverThread: Thread = object : Thread() {
            override fun run() {
                webServer = MiddlewareWebServer.Builder()
                        .hostName("localHost")
                        .resourcePath("storage/emulated/0/Download/test")
                        .httpsPort(8443)
                        .httpPort(8080)
                        .wssPort(9999)
                        .wsPort(9998)
                        .addRPCProcessor(rpcProcessor)
                        .sslContext(UserAgentSSLContext(applicationContext))
                        .enableConnectors(
                                arrayOf(
                                        MiddlewareWebServer.Connectors.HTTPS_CONNECTOR,
                                        MiddlewareWebServer.Connectors.HTTP_CONNECTOR,
                                        MiddlewareWebServer.Connectors.WS_CONNECTOR,
                                        MiddlewareWebServer.Connectors.WSS_CONNECTOR
                                )
                        )
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
        }
        stopSelf()
        isServiceStarted = false
    }

    companion object {
        const val ACTION_START: String = "START"
        const val ACTION_STOP: String = "STOP"

        private const val NOTIFICATION_CHANNEL_ID = "ForegroundRpcServiceChannel"
        private const val NOTIFICATION_ID = 1
    }
}