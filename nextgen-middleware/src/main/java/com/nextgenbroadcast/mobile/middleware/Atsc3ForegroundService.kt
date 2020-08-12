package com.nextgenbroadcast.mobile.middleware

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import androidx.lifecycle.LifecycleService
import com.nextgenbroadcast.mobile.core.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.core.unite
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3Module
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.service.ServiceControllerImpl
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.controller.view.ViewControllerImpl
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.RPCGatewayImpl
import com.nextgenbroadcast.mobile.middleware.gateway.web.IWebGateway
import com.nextgenbroadcast.mobile.middleware.gateway.web.WebGatewayImpl
import com.nextgenbroadcast.mobile.middleware.notification.NotificationHelper
import com.nextgenbroadcast.mobile.middleware.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IUserAgentPresenter
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.repository.RepositoryImpl
import com.nextgenbroadcast.mobile.middleware.web.MiddlewareWebServer
import kotlinx.coroutines.Dispatchers

class Atsc3ForegroundService : LifecycleService() {
    private lateinit var repository: IRepository
    private lateinit var atsc3Module: Atsc3Module
    private lateinit var serviceController: IServiceController
    private lateinit var notificationHelper: NotificationHelper

    private var viewController: IViewController? = null
    private var webGateway: IWebGateway? = null
    private var rpcGateway: IRPCGateway? = null

    private lateinit var wakeLock: WakeLock

    private var isServiceStarted: Boolean = false
    private var webServer: MiddlewareWebServer? = null

    override fun onCreate() {
        super.onCreate()

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Atsc3ForegroundService::lock")

        val repo = RepositoryImpl().also {
            repository = it
        }
        val atsc3 = Atsc3Module(this).also {
            atsc3Module = it
        }
        serviceController = ServiceControllerImpl(repo, atsc3)
        notificationHelper = NotificationHelper(this, NOTIFICATION_CHANNEL_ID).also {
            it.createNotificationChannel(getString(R.string.atsc3_chanel_name))
        }

        startForeground(NOTIFICATION_ID, checkAtsc3SourceStateAndCreateNotification())
    }

    private fun checkAtsc3SourceStateAndCreateNotification(title: String = "", message: String = "", state: PlaybackState = PlaybackState.IDLE): Notification {
        return if (getAtsc3SourceState() == ReceiverState.OPENED) {

            val titleValue = if (title.isEmpty()) getServiceName() else title

            createNotification(titleValue, message, state)
        } else {
            createAtsc3SourceChooserNotification()
        }
    }

    private fun createAtsc3SourceChooserNotification(): Notification {

        val notificationIntent = Intent(this, Atsc3NotificationDialogActivity::class.java)
        val contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        return createNotification(getString(R.string.atsc3_source_is_not_initialized)).apply {
            this.contentIntent = contentIntent
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent != null) {
            when (intent.action) {
                ACTION_START -> startService()

                ACTION_STOP -> stopService()

                ACTION_RMP_PLAY -> viewController?.rmpResume()

                ACTION_RMP_PAUSE -> viewController?.rmpPause()

                ACTION_ATSC3_SOURCE_OPEN -> {
                    openAtsc3Source(intent)
                }

                else -> {
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun openAtsc3Source(intent: Intent) {
        val pcapFilePath = intent.getStringExtra("pcapSourcePath")
        serviceController.openRoute(pcapFilePath)
    }

    @Deprecated("old implementation")
    private fun startService() {
        if (isServiceStarted) return
        isServiceStarted = true

//        wakeLock.acquire()
//        startWebServer()
    }

    @Deprecated("old implementation")
    private fun stopService() {
        if (isServiceStarted) {
            wakeLock.release()

            stopWebServer()
            stopForeground(true)
        }
        stopSelf()
        isServiceStarted = false
    }

    private fun startWebServer(rpc: IRPCGateway, web: IWebGateway) {
        webServer = MiddlewareWebServer.Builder()
                .hostName(web.hostName)
                .httpsPort(web.httpsPort)
                .httpPort(web.httpPort)
                .wssPort(web.wssPort)
                .wsPort(web.wsPort)
                .rpcGateway(rpc)
                .webGateway(web)
                .sslContext(UserAgentSSLContext(applicationContext))
                .build().also {
                    it.start()
                }
    }

    private fun stopWebServer() {
        webServer?.let { server ->
            if (server.isRunning()) {
                try {
                    server.stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)

        createViewPresentationAndStartService()

        return ServiceBinder()
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)

        createViewPresentationAndStartService()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        super.onUnbind(intent)

        destroyViewPresentationAndStopService()

        return true
    }

    private fun createViewPresentationAndStartService() {
        val view = ViewControllerImpl(repository).also {
            viewController = it
        }
        val web = WebGatewayImpl(serviceController, repository).also {
            webGateway = it
        }
        val rpc = RPCGatewayImpl(serviceController, view, Dispatchers.Main, Dispatchers.IO).also {
            rpcGateway = it
        }

        web.selectedService.unite(view.rmpState).observe(this, androidx.lifecycle.Observer { (service, state) ->
            updateNotification(service, state)
        })

        //TODO: start only when atsc3 source active?!
        startWebServer(rpc, web)

        //TODO: add lock limitation??
        wakeLock.acquire()
    }

    private fun destroyViewPresentationAndStopService() {
        wakeLock.release()

        stopWebServer()

        webGateway = null
        rpcGateway = null
        viewController = null
    }

    private fun getAtsc3SourceState() = serviceController.receiverState.value ?: ReceiverState.IDLE
    private fun getServiceName() = webGateway?.let { it.selectedService.value?.shortName } ?: getString(R.string.atsc3_no_service_available)

    private fun createNotification(title: String, message: String = "", playbackState: PlaybackState = PlaybackState.IDLE): Notification {
        return notificationHelper.createMediaNotification(title, message, playbackState)
    }

    private fun updateNotification(service: SLSService?, rmpState: PlaybackState?) {
        val serviceName = service?.shortName ?: getString(R.string.atsc3_no_service_available)
        val playbackState = rmpState ?: PlaybackState.IDLE
        notificationHelper.notify(NOTIFICATION_ID, checkAtsc3SourceStateAndCreateNotification(title = serviceName, state = playbackState))
    }

    inner class ServiceBinder : Binder() {
        fun getReceiverPresenter(): IReceiverPresenter = serviceController
        fun getSelectorPresenter(): ISelectorPresenter = serviceController
        fun getUserAgentPresenter(): IUserAgentPresenter = viewController.required()
        fun getMediaPlayerPresenter(): IMediaPlayerPresenter = viewController.required()
    }

    private fun <T> T?.required(): T {
        return this ?: throw InitializationException()
    }

    class InitializationException : RuntimeException()

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "Atsc3ServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val SERVICE_ACTION = "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action"

        const val ACTION_START = "$SERVICE_ACTION.START"
        const val ACTION_STOP = "$SERVICE_ACTION.STOP"
        const val ACTION_RMP_PLAY = "$SERVICE_ACTION.RMP_PLAY"
        const val ACTION_RMP_PAUSE = "$SERVICE_ACTION.RMP_PAUSE"
        const val ACTION_ATSC3_SOURCE_OPEN = "$SERVICE_ACTION.ATSC3_SOURCE_OPEN"
    }
}