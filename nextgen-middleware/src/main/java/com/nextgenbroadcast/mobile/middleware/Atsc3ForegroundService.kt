package com.nextgenbroadcast.mobile.middleware

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.nextgenbroadcast.mobile.core.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.model.SLSService
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
import com.nextgenbroadcast.mobile.middleware.phy.Atsc3DeviceReceiver
import com.nextgenbroadcast.mobile.middleware.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IUserAgentPresenter
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import com.nextgenbroadcast.mobile.middleware.settings.MiddlewareSettingsImpl
import com.nextgenbroadcast.mobile.middleware.repository.RepositoryImpl
import com.nextgenbroadcast.mobile.middleware.server.web.MiddlewareWebServer
import kotlinx.coroutines.Dispatchers

class Atsc3ForegroundService : LifecycleService() {
    private lateinit var settings: IMiddlewareSettings
    private lateinit var repository: IRepository
    private lateinit var atsc3Module: Atsc3Module
    private lateinit var serviceController: IServiceController
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var state: MediatorLiveData<Triple<ReceiverState?, SLSService?, PlaybackState?>>

    private var viewController: IViewController? = null
    private var webGateway: IWebGateway? = null
    private var rpcGateway: IRPCGateway? = null

    private lateinit var wakeLock: WakeLock

    private var webServer: MiddlewareWebServer? = null
    private var deviceReceiver: Atsc3DeviceReceiver? = null
    private var isForeground = false
    private var isBinded = false

    override fun onCreate() {
        super.onCreate()

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Atsc3ForegroundService::lock")

        settings = MiddlewareSettingsImpl(applicationContext)

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

        state = MediatorLiveData<Triple<ReceiverState?, SLSService?, PlaybackState?>>().apply {
            addSource(serviceController.receiverState) { receiverState ->
                value = newState(receiverState = receiverState)
            }
            addSource(serviceController.selectedService) { service ->
                value = newState(selectedService = service)
            }
        }.also {
            it.observe(this, Observer { (receiverState, selectedService, playbackState) ->
                notificationHelper.notify(NOTIFICATION_ID, createNotification(receiverState, selectedService, playbackState))
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent != null) {
            when (intent.action) {
                ACTION_START -> startService()

                ACTION_STOP -> killService()

                ACTION_DEVICE_ATTACHED -> onDeviceAttached(intent.getParcelableExtra<UsbDevice?>(UsbManager.EXTRA_DEVICE))

                ACTION_DEVICE_DETACHED -> onDeviceDetached(intent.getParcelableExtra<UsbDevice?>(UsbManager.EXTRA_DEVICE))

                ACTION_RMP_PLAY -> viewController?.rmpResume()

                ACTION_RMP_PAUSE -> viewController?.rmpPause()

                ACTION_OPEN_FILE -> openFileSource(intent.getStringExtra(EXTRA_FILE_PATH))

                ACTION_CLOSE_ROUTE -> closeRoute()

                else -> {
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun openFileSource(filePath: String?) {
        // change source to file. So, let's unregister device receiver
        unregisterDeviceReceiver()

        filePath?.let {
            serviceController.openRoute(filePath)
        }
    }

    private fun closeRoute() {
        unregisterDeviceReceiver()
        serviceController.closeRoute()

        if (isBinded) {
            stopSelf()
        } else {
            killService()
        }
    }

    private fun startService() {
        if (isForeground) return
        isForeground = true

        startForeground(NOTIFICATION_ID, createNotification(getReceiverState()))
    }

    private fun stopService() {
        stopForeground(true)

        isForeground = false
    }

    private fun killService() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        unregisterDeviceReceiver()
        stopWebServer()
        stopService()
        stopSelf()
    }

    private fun onDeviceAttached(device: UsbDevice?) {
        if (device == null) {
            if (!isForeground && !isBinded) {
                stopSelf()
            }
            return
        }

        //TODO: process case with second connected device

        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        if (usbManager.hasPermission(device)) {
            startService()
            unregisterDeviceReceiver()

            serviceController.openRoute(device, usbManager)

            // Register BroadcastReceiver to detect when device is disconnected
            deviceReceiver = Atsc3DeviceReceiver(device.deviceName).also { receiver ->
                registerReceiver(receiver, IntentFilter().apply {
                    addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                })
            }
        }
    }

    private fun onDeviceDetached(device: UsbDevice?) {
        closeRoute()
    }

    private fun unregisterDeviceReceiver() {
        deviceReceiver?.let { receiver ->
            unregisterReceiver(receiver)
            deviceReceiver = null
        }
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

        startService()

        createViewPresentationAndStartService()

        isBinded = true

        return ServiceBinder()
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)

        isBinded = true
    }

    override fun onUnbind(intent: Intent?): Boolean {
        super.onUnbind(intent)

        isBinded = false

        destroyViewPresentationAndStopService()

        return true
    }

    private fun createViewPresentationAndStartService() {
        val view = ViewControllerImpl(repository, settings).also {
            viewController = it
        }
        val web = WebGatewayImpl(serviceController, repository, settings).also {
            webGateway = it
        }
        val rpc = RPCGatewayImpl(serviceController, view, settings, Dispatchers.Main, Dispatchers.IO).also {
            rpcGateway = it
        }

        state.addSource(view.rmpState) { playbackState ->
            state.value = newState(playbackState = playbackState)
        }

        startWebServer(rpc, web)

        //TODO: add lock limitation??
        wakeLock.acquire()
    }

    private fun destroyViewPresentationAndStopService() {
        viewController?.let { view ->
            state.removeSource(view.rmpState)
        }

        wakeLock.release()

        stopWebServer()

        webGateway = null
        rpcGateway = null
        viewController = null
    }

    private fun requireViewController(): IViewController {
        if (viewController == null) {
            createViewPresentationAndStartService()
        }
        return viewController ?: throw InitializationException()
    }

    private fun newState(receiverState: ReceiverState? = null, selectedService: SLSService? = null, playbackState: PlaybackState? = null) = Triple(
            receiverState ?: serviceController.receiverState.value,
            selectedService ?: serviceController.selectedService.value,
            playbackState ?: viewController?.rmpState?.value
    )

    private fun getReceiverState() = serviceController.receiverState.value ?: ReceiverState.IDLE

    private fun createNotification(state: ReceiverState? = null, service: SLSService? = null, playbackState: PlaybackState? = null): Notification {
        val title = if (state == null || state == ReceiverState.IDLE) {
            getString(R.string.atsc3_source_is_not_initialized)
        } else {
            service?.shortName ?: getString(R.string.atsc3_no_service_available)
        }

        return notificationHelper.createMediaNotification(title, "", playbackState
                ?: PlaybackState.IDLE)
    }

    inner class ServiceBinder : Binder() {
        fun getReceiverPresenter(): IReceiverPresenter = object : IReceiverPresenter {
            override val receiverState = serviceController.receiverState

            override fun openRoute(pcapFile: String): Boolean {
                openFile(this@Atsc3ForegroundService, pcapFile)
                return true
            }

            override fun closeRoute() {
                // call to stopRoute is not a mistake. We use it to close previously opened file
                serviceController.stopRoute()
                closeRoute(this@Atsc3ForegroundService)
            }
        }
        fun getSelectorPresenter(): ISelectorPresenter = serviceController
        fun getUserAgentPresenter(): IUserAgentPresenter = requireViewController()
        fun getMediaPlayerPresenter(): IMediaPlayerPresenter = requireViewController()
    }

    class InitializationException : RuntimeException()

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "Atsc3ServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val SERVICE_ACTION = "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action"

        @Deprecated("old implementation")
        const val ACTION_START = "$SERVICE_ACTION.START"
        @Deprecated("old implementation")
        const val ACTION_STOP = "$SERVICE_ACTION.STOP"
        const val ACTION_DEVICE_ATTACHED = "$SERVICE_ACTION.USB_ATTACHED"
        const val ACTION_DEVICE_DETACHED = "$SERVICE_ACTION.USB_DETACHED"
        const val ACTION_RMP_PLAY = "$SERVICE_ACTION.RMP_PLAY"
        const val ACTION_RMP_PAUSE = "$SERVICE_ACTION.RMP_PAUSE"
        const val ACTION_OPEN_FILE = "$SERVICE_ACTION.OPEN_FILE"
        const val ACTION_CLOSE_ROUTE = "$SERVICE_ACTION.CLOSE_ROUTE"

        const val EXTRA_DEVICE = "device"
        const val EXTRA_FILE_PATH = "file_path"

        @Deprecated("old implementation")
        fun startService(context: Context) {
            ContextCompat.startForegroundService(context, newIntent(context, ACTION_START))
        }

        @Deprecated("old implementation")
        fun stopService(context: Context) {
            ContextCompat.startForegroundService(context, newIntent(context, ACTION_STOP))
        }

        fun startForDevice(context: Context, device: UsbDevice) {
            newIntent(context, ACTION_DEVICE_ATTACHED).let { serviceIntent ->
                serviceIntent.putExtra(EXTRA_DEVICE, device)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }

        fun stopForDevice(context: Context, device: UsbDevice) {
            newIntent(context, ACTION_DEVICE_DETACHED).let { serviceIntent ->
                serviceIntent.putExtra(EXTRA_DEVICE, device)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }

        fun openFile(context: Context, filePath: String) {
            newIntent(context, ACTION_OPEN_FILE).let { serviceIntent ->
                serviceIntent.putExtra(EXTRA_FILE_PATH, filePath)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }

        fun closeRoute(context: Context) {
            ContextCompat.startForegroundService(context, newIntent(context, ACTION_CLOSE_ROUTE))
        }

        private fun newIntent(context: Context, serviceAction: String) = Intent(context, Atsc3ForegroundService::class.java).apply {
            action = serviceAction
        }
    }
}