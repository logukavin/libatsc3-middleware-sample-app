package com.nextgenbroadcast.mobile.middleware.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import androidx.core.content.ContextCompat
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.nextgenbroadcast.mobile.core.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.middleware.BuildConfig
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
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.repository.RepositoryImpl
import com.nextgenbroadcast.mobile.middleware.server.web.MiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.service.init.UsbPhyInitializer
import com.nextgenbroadcast.mobile.middleware.service.init.OnboardPhyInitializer
import com.nextgenbroadcast.mobile.middleware.service.init.IServiceInitializer
import com.nextgenbroadcast.mobile.middleware.service.init.FrequencyInitializer
import com.nextgenbroadcast.mobile.middleware.service.init.MetadataReader
import com.nextgenbroadcast.mobile.middleware.service.provider.IMediaFileProvider
import com.nextgenbroadcast.mobile.middleware.service.provider.MediaFileProvider
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import com.nextgenbroadcast.mobile.middleware.settings.MiddlewareSettingsImpl
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

abstract class Atsc3ForegroundService : BindableForegroundService() {
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

    private var isInitialized = false
    private val initializer = ArrayList<WeakReference<IServiceInitializer>>()

    private val usbManager: UsbManager by lazy {
        getSystemService(Context.USB_SERVICE) as UsbManager
    }

    protected open val mediaFileProvider: IMediaFileProvider by lazy {
        MediaFileProvider(applicationContext)
    }

    abstract fun createServiceBinder(serviceController: IServiceController, viewController: IViewController): IBinder

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

        serviceController = ServiceControllerImpl(repo, settings, atsc3)

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

    override fun onDestroy() {
        super.onDestroy()

        initializer.forEach { ref ->
            ref.get()?.cancel()
        }

        atsc3Module.close()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)

        maybeInitialize()

        return createServiceBinder(serviceController, requireViewController())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent != null) {
            when (intent.action) {
                ACTION_START -> startForeground()

                ACTION_STOP -> killService()

                ACTION_DEVICE_ATTACHED -> onDeviceAttached(intent.getParcelableExtra(UsbManager.EXTRA_DEVICE))

                ACTION_DEVICE_DETACHED -> onDeviceDetached(intent.getParcelableExtra(UsbManager.EXTRA_DEVICE))

                ACTION_USB_PERMISSION -> intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false).let { granted ->
                    if (granted) {
                        onDevicePermissionGranted(intent.getParcelableExtra(UsbManager.EXTRA_DEVICE))
                    }
                }

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

    private fun maybeInitialize() {
        if (isInitialized) return

        isInitialized = true

        val components = MetadataReader.discoverMetadata(this)

        FrequencyInitializer(settings, serviceController).also {
            initializer.add(WeakReference(it))
        }.initialize(applicationContext, components)

        val phyInitializer = OnboardPhyInitializer(atsc3Module).also {
            initializer.add(WeakReference(it))
        }

        if (!phyInitializer.initialize(applicationContext, components)) {
            UsbPhyInitializer().also {
                initializer.add(WeakReference(it))
            }.initialize(applicationContext, components)
        }
    }

    private fun openRoute(filePath: String?) {
        // change source to file. So, let's unregister device receiver
        unregisterDeviceReceiver()

        filePath?.let {
            serviceController.openRoute(filePath)
        }
    }

    private fun openRoute(device: UsbDevice) {
        startForeground()
        unregisterDeviceReceiver()

        serviceController.openRoute(device)

        // Register BroadcastReceiver to detect when device is disconnected
        deviceReceiver = Atsc3DeviceReceiver(device.deviceName).also { receiver ->
            registerReceiver(receiver, IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            })
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
        if (usbManager.hasPermission(device)) {
            openRoute(device)
        } else {
            requestDevicePermission(device)
        }
    }

    private fun onDeviceDetached(device: UsbDevice?) {
        closeRoute()
    }

    private fun onDevicePermissionGranted(device: UsbDevice?) {
        device?.let {
            // open device using a new Intent to start Service as foreground
            startForDevice(this, device)
        }
    }

    private fun unregisterDeviceReceiver() {
        deviceReceiver?.let { receiver ->
            unregisterReceiver(receiver)
            deviceReceiver = null
        }
    }

    private fun requestDevicePermission(device: UsbDevice) {
        val intent = Intent(this, clazz).apply {
            action = ACTION_USB_PERMISSION
        }
        usbManager.requestPermission(device, PendingIntent.getService(this, 0, intent, 0))
    }

    private fun startWebServer(rpc: IRPCGateway, web: IWebGateway) {
        webServer = MiddlewareWebServer.Builder()
                .rpcGateway(rpc)
                .webGateway(web)
                .build().also {
                    it.start(UserAgentSSLContext(applicationContext))
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

        val view = ViewControllerImpl(repository, settings, mediaFileProvider).also {
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

        if (wakeLock.isHeld) {
            wakeLock.release()
        }

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
        const val ACTION_OPEN_ROUTE = "$SERVICE_ACTION.OPEN_ROUTE"
        const val ACTION_CLOSE_ROUTE = "$SERVICE_ACTION.CLOSE_ROUTE"

        const val EXTRA_DEVICE = UsbManager.EXTRA_DEVICE
        const val EXTRA_ROUTE_PATH = "route_path"

        internal lateinit var clazz: Class<out Atsc3ForegroundService>

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

        private fun newIntent(context: Context, serviceAction: String) = Intent(context, clazz).apply {
            action = serviceAction
            putExtra(EXTRA_FOREGROUND, true)
        }
    }
}