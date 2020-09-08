package com.nextgenbroadcast.mobile.middleware

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.*
import android.os.PowerManager.WakeLock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.nextgenbroadcast.mobile.core.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.RPMParams
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
import com.nextgenbroadcast.mobile.middleware.phy.Atsc3DeviceReceiver
import com.nextgenbroadcast.mobile.middleware.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IUserAgentPresenter
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.repository.RepositoryImpl
import com.nextgenbroadcast.mobile.middleware.server.web.MiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import com.nextgenbroadcast.mobile.middleware.settings.MiddlewareSettingsImpl
import kotlinx.coroutines.Dispatchers


class Atsc3ForegroundService : BindableForegroundService() {
    private lateinit var wakeLock: WakeLock
    private lateinit var settings: IMiddlewareSettings
    private lateinit var repository: IRepository
    private lateinit var atsc3Module: Atsc3Module
    private lateinit var serviceController: IServiceController
    private lateinit var state: MediatorLiveData<Triple<ReceiverState?, SLSService?, PlaybackState?>>

    private var viewController: IViewController? = null
    private var webGateway: IWebGateway? = null
    private var rpcGateway: IRPCGateway? = null
    private var webServer: MiddlewareWebServer? = null
    private var deviceReceiver: Atsc3DeviceReceiver? = null

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

        state = MediatorLiveData<Triple<ReceiverState?, SLSService?, PlaybackState?>>().apply {
            addSource(serviceController.receiverState) { receiverState ->
                value = newState(receiverState = receiverState)
            }
            addSource(serviceController.selectedService) { service ->
                value = newState(selectedService = service)
            }
        }.also {
            it.observe(this, Observer { (receiverState, selectedService, playbackState) ->
                if (isForeground) {
                    pushNotification(createNotification(receiverState, selectedService, playbackState))
                }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent != null) {
            when (intent.action) {
                ACTION_START -> startForeground()

                ACTION_STOP -> killService()

                ACTION_DEVICE_ATTACHED -> onDeviceAttached(intent.getParcelableExtra<UsbDevice?>(UsbManager.EXTRA_DEVICE))

                ACTION_DEVICE_DETACHED -> onDeviceDetached(intent.getParcelableExtra<UsbDevice?>(UsbManager.EXTRA_DEVICE))

                ACTION_RMP_PLAY -> viewController?.rmpResume()

                ACTION_RMP_PAUSE -> viewController?.rmpPause()

                ACTION_OPEN_ROUTE -> openRoute(intent.getStringExtra(EXTRA_ROUTE_PATH))

                ACTION_CLOSE_ROUTE -> closeRoute()

                else -> {
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return mMessenger.binder
        //return ServiceBinder()
    }

    private fun openRoute(filePath: String?) {
        // change source to file. So, let's unregister device receiver
        unregisterDeviceReceiver()

        filePath?.let {
            serviceController.openRoute(filePath)
        }
    }

    private fun closeRoute() {
        unregisterDeviceReceiver()

        serviceController.stopRoute() // call to stopRoute is not a mistake. We use it to close previously opened file
        serviceController.closeRoute()

        if (isBinded) {
            stopSelf()
        } else {
            killService()
        }
    }

    private fun killService() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        unregisterDeviceReceiver()
        stopWebServer()
        stopForeground()
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
            startForeground()
            unregisterDeviceReceiver()

            serviceController.openRoute(device)

            // Register BroadcastReceiver to detect when device is disconnected
            deviceReceiver = Atsc3DeviceReceiver(device.deviceName).also { receiver ->
                registerReceiver(receiver, IntentFilter().apply {
                    addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                })
            }
        } else {
            //TODO: If we need this request then add ACTION_USB_PERMISSION action processing
            usbManager.requestPermission(device, PendingIntent.getService(this, 0, Intent(ACTION_USB_PERMISSION), 0))
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

    override fun getReceiverState() = serviceController.receiverState.value ?: ReceiverState.IDLE

    override fun createViewPresentationAndStartService() {
        // we release it only when destroy presentation layer
        if (wakeLock.isHeld) return

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

    override fun destroyViewPresentationAndStopService() {
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

    inner class ServiceBinder : Binder(), IServiceBinder {
        override fun getReceiverPresenter(): IReceiverPresenter = object : IReceiverPresenter {
            override val receiverState = serviceController.receiverState

            override fun openRoute(path: String): Boolean {
                openRoute(this@Atsc3ForegroundService, path)
                return true
            }

            override fun closeRoute() {
                closeRoute(this@Atsc3ForegroundService)
            }
        }
        override fun getSelectorPresenter(): ISelectorPresenter = serviceController
        override fun getUserAgentPresenter(): IUserAgentPresenter = requireViewController()
        override fun getMediaPlayerPresenter(): IMediaPlayerPresenter = requireViewController()
    }

    val mMessenger = Messenger(IncomingHandler())

    inner class IncomingHandler : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            Log.d("TEST", "to service msg: ${msg.what}")
            when (msg.what) {
                RECEIVER_STATE -> sendReceiverState(msg.replyTo)
                OPEN_ROUTE_STATE -> {
                    val path = msg.data.getString("OPEN_ROUTE_STATE")
                    Log.d("TEST", "path: $path")
                    path?.let {
                        openRoute(this@Atsc3ForegroundService, path)
                    }
                }
                CLOSE_ROUTE_STATE -> {
                    closeRoute(this@Atsc3ForegroundService)
                }
                SLT_SERVICE_STATE -> sendSltServiceState(msg.replyTo)
                SELECTED_SERVICE_STATE -> sendSelectedServiceState(msg.replyTo)
                SELECT_SERVICE_STATE -> {
                    msg.data.classLoader = SLSService::class.java.classLoader
                    val service = msg.data.getParcelable<SLSService?>("SELECT_SERVICE_STATE")
                    service?.let {
                        Log.d("TEST", "Select: ${service.shortName}")
                        serviceController.selectService(service)
                    }
                }
                APP_DATA_SERVICE_STATE -> {
                    sendAppData(msg.replyTo)
                }
                RMP_LAYOUT_PARAMS -> sendRPMLayoutParams(msg.replyTo)
                RMP_MEDIA_URL -> sendRPMMediaUrl(msg.replyTo)
                RMP_LAYOUT_RESET -> {
                    requireViewController().rmpLayoutReset()
                }
                RMP_PLAYBACK_CHECKED -> {
                    msg.data.classLoader = PlaybackState::class.java.classLoader
                    val playBackState = msg.data.getParcelable<PlaybackState?>("RMP_PLAYBACK_CHECKED")
                    playBackState?.let {
                        requireViewController().rmpPlaybackChanged(playBackState)
                    }
                }
                RMP_PLAYBACK_RATE_CHANGED -> {
                    val speed = msg.data.getFloat("RMP_PLAYBACK_RATE_CHANGED")
                    requireViewController().rmpPlaybackRateChanged(speed)
                }
                RMP_MEDIA_TIME_CHANGED -> {
                    val currentTime = msg.data.getLong("RMP_MEDIA_TIME_CHANGED")
                    requireViewController().rmpMediaTimeChanged(currentTime)
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    private fun sendReceiverState(messenger: Messenger) {
        serviceController.receiverState.observe(this, Observer { state ->
            val msg: Message = Message.obtain(null, RECEIVER_STATE, 0, 0)
            msg.data.classLoader = ReceiverState::class.java.classLoader
            msg.data.putParcelable("RECEIVER_STATE", state)
            Log.d("TEST", "Send state: ${state.name}")
            try {
                messenger.send(msg)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        })
    }

    private fun sendSltServiceState(messenger: Messenger) {
        serviceController.sltServices.observe(this, Observer { services ->
            val msg: Message = Message.obtain(null, SLT_SERVICE_STATE, 0, 0)
            msg.data.classLoader = SLSService::class.java.classLoader
            msg.data.putParcelableArrayList("SLT_SERVICE_STATE", ArrayList<SLSService>(services))
            Log.d("TEST", "Send services: $services")
            try {
                messenger.send(msg)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        })
    }

    private fun sendSelectedServiceState(messenger: Messenger) {
        serviceController.selectedService.observe(this, Observer { selectedService ->
            val msg: Message = Message.obtain(null, SELECTED_SERVICE_STATE, 0, 0)
            msg.data.classLoader = SLSService::class.java.classLoader
            msg.data.putParcelable("SELECTED_SERVICE_STATE", selectedService)
            Log.d("TEST", "Send selected: $selectedService")
            try {
                messenger.send(msg)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        })
    }

    private fun sendAppData(messenger: Messenger) {
        requireViewController().appData.observe(this, Observer { appData ->
            val msg: Message = Message.obtain(null, APP_DATA_SERVICE_STATE, 0, 0)
            msg.data.putParcelable("app_data", appData)
            Log.d("TEST", "Send appData: $appData")
            try {
                messenger.send(msg)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        })
    }

    private fun sendRPMLayoutParams(messenger: Messenger) {
        requireViewController().rmpLayoutParams.observe(this, Observer { rpmLayoutParams ->
            val msg: Message = Message.obtain(null, RMP_LAYOUT_PARAMS, 0, 0)
            msg.data.classLoader = RPMParams::class.java.classLoader
            msg.data.putParcelable("rpm_layout_params", rpmLayoutParams)
            Log.d("TEST", "Send rpm layout params: $rpmLayoutParams")
            try {
                messenger.send(msg)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        })
    }

    private fun sendRPMMediaUrl(messenger: Messenger) {
        requireViewController().rmpMediaUrl.observe(this, Observer { rmpMediaUrl ->
            val msg: Message = Message.obtain(null, RMP_MEDIA_URL, 0, 0)
            msg.data.classLoader = String::class.java.classLoader
            msg.data.putString("rpm_media_url", rmpMediaUrl)
            Log.d("TEST", "Send rpm media url: $rmpMediaUrl")
            try {
                messenger.send(msg)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        })
    }

    class InitializationException : RuntimeException()

    companion object {
        private const val SERVICE_ACTION = "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action"

        @Deprecated("old implementation")
        const val ACTION_START = "$SERVICE_ACTION.START"
        @Deprecated("old implementation")
        const val ACTION_STOP = "$SERVICE_ACTION.STOP"
        const val ACTION_DEVICE_ATTACHED = "$SERVICE_ACTION.USB_ATTACHED"
        const val ACTION_DEVICE_DETACHED = "$SERVICE_ACTION.USB_DETACHED"
        const val ACTION_USB_PERMISSION = "$SERVICE_ACTION.USB_PERMISSION"
        const val ACTION_RMP_PLAY = "$SERVICE_ACTION.RMP_PLAY"
        const val ACTION_RMP_PAUSE = "$SERVICE_ACTION.RMP_PAUSE"
        const val ACTION_OPEN_ROUTE = "$SERVICE_ACTION.OPEN_FILE"
        const val ACTION_CLOSE_ROUTE = "$SERVICE_ACTION.CLOSE_ROUTE"

        const val EXTRA_DEVICE = "device"
        const val EXTRA_ROUTE_PATH = "file_path"

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

        fun openRoute(context: Context, filePath: String) {
            newIntent(context, ACTION_OPEN_ROUTE).let { serviceIntent ->
                serviceIntent.putExtra(EXTRA_ROUTE_PATH, filePath)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }

        fun closeRoute(context: Context) {
            ContextCompat.startForegroundService(context, newIntent(context, ACTION_CLOSE_ROUTE))
        }

        private fun newIntent(context: Context, serviceAction: String) = Intent(context, Atsc3ForegroundService::class.java).apply {
            action = serviceAction
            putExtra(EXTRA_FOREGROUND, true)
        }
    }
}