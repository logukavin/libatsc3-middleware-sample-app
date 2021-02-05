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
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.MediatorLiveData
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.middleware.BuildConfig
import com.nextgenbroadcast.mobile.middleware.atsc3.source.UsbAtsc3Source
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.phy.Atsc3DeviceReceiver
import com.nextgenbroadcast.mobile.middleware.service.core.Atsc3ServiceCore
import com.nextgenbroadcast.mobile.middleware.service.init.*
import com.nextgenbroadcast.mobile.middleware.settings.MiddlewareSettingsImpl
import kotlinx.coroutines.*

abstract class Atsc3ForegroundService : BindableForegroundService() {
    private lateinit var atsc3Service: Atsc3ServiceCore
    private lateinit var wakeLock: WakeLock
    private lateinit var state: MediatorLiveData<Triple<ReceiverState?, AVService?, PlaybackState?>>

    private var viewPresentationLifecycle: ViewPresentationLifecycleOwner? = null
    private var deviceReceiver: Atsc3DeviceReceiver? = null

    private var isInitialized = false

    private val usbManager: UsbManager by lazy {
        getSystemService(Context.USB_SERVICE) as UsbManager
    }

    private var destroyPresentationLayerJob: Job? = null

    abstract fun createServiceBinder(serviceController: IServiceController): IBinder

    override fun onCreate() {
        super.onCreate()

        val settings = MiddlewareSettingsImpl.getInstance(applicationContext)
        atsc3Service = Atsc3ServiceCore(applicationContext, settings)

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Atsc3ForegroundService::lock")

        state = MediatorLiveData<Triple<ReceiverState?, AVService?, PlaybackState?>>().apply {
            addSource(atsc3Service.serviceController.receiverState) { receiverState ->
                value = newState(receiverState = receiverState)
            }
            addSource(atsc3Service.serviceController.selectedService) { service ->
                value = newState(selectedService = service)
            }
        }.also {
            it.observe(this, { (receiverState, selectedService, playbackState) ->
                if (isForeground) {
                    pushNotification(createNotification(receiverState, selectedService, playbackState))
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        destroyViewPresentationAndStopService()

        atsc3Service.destroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        cancelPresentationDestroying()
        createViewPresentationAndStartService()

        super.onBind(intent)

        maybeInitialize()

        return createServiceBinder(atsc3Service.serviceController)
    }

    override fun onRebind(intent: Intent?) {
        cancelPresentationDestroying()

        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        cancelPresentationDestroying()
        if (isStartedAsForeground) {
            startPresentationDestroying()
        } else {
            destroyViewPresentationAndStopService()
        }

        return super.onUnbind(intent)
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

                ACTION_RMP_PLAY -> atsc3Service.viewController?.rmpResume()

                ACTION_RMP_PAUSE -> atsc3Service.viewController?.rmpPause()

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

        try {
            atsc3Service.initialize(MetadataReader.discoverMetadata(this))
        } catch (e: Exception) {
            Log.d(TAG, "Can't initialize, something is wrong in metadata", e)
        }
    }

    private fun openRoute(filePath: String?) {
        // change source to file. So, let's unregister device receiver
        unregisterDeviceReceiver()

        filePath?.let {
            atsc3Service.openRoute(filePath)
        }
    }

    private fun openRoute(device: UsbDevice) {
        startForeground()
        unregisterDeviceReceiver()

        atsc3Service.openRoute(UsbAtsc3Source(usbManager, device))

        // Register BroadcastReceiver to detect when device is disconnected
        deviceReceiver = Atsc3DeviceReceiver(device.deviceName).also { receiver ->
            registerReceiver(receiver, IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            })
        }
    }

    private fun closeRoute() {
        unregisterDeviceReceiver()

        atsc3Service.closeRoute()

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
        atsc3Service.stopAndDestroyViewPresentation()
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

    override fun getReceiverState() = atsc3Service.getReceiverState()

    private fun createViewPresentationAndStartService() {
        // we release it only when destroy presentation layer
        if (wakeLock.isHeld) return

        viewPresentationLifecycle?.stopAndDestroy()
        val viewLifecycle = ViewPresentationLifecycleOwner().apply {
            start()
        }.also {
            viewPresentationLifecycle = it
        }

        val view = atsc3Service.createAndStartViewPresentation(viewLifecycle)

        state.addSource(view.rmpState) { playbackState ->
            state.value = newState(playbackState = playbackState)
        }

        //TODO: add lock limitation??
        wakeLock.acquire()
    }

    private fun destroyViewPresentationAndStopService() {
        viewPresentationLifecycle?.stopAndDestroy()
        viewPresentationLifecycle = null

        atsc3Service.viewController?.let { view ->
            state.removeSource(view.rmpState)
        }

        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        atsc3Service.stopAndDestroyViewPresentation()
    }

    private fun startPresentationDestroying() {
        destroyPresentationLayerJob = CoroutineScope(Dispatchers.IO).launch {
            delay(PRESENTATION_DESTROYING_DELAY)
            withContext(Dispatchers.Main) {
                destroyViewPresentationAndStopService()
                destroyPresentationLayerJob = null
            }
        }
    }

    private fun cancelPresentationDestroying() {
        destroyPresentationLayerJob?.let {
            it.cancel()
            destroyPresentationLayerJob = null
        }
    }

    protected fun requireViewController(): IViewController {
        if (atsc3Service.viewController == null) {
            createViewPresentationAndStartService()
        }
        return atsc3Service.viewController ?: throw InitializationException()
    }

    private fun newState(receiverState: ReceiverState? = null, selectedService: AVService? = null, playbackState: PlaybackState? = null) = with(atsc3Service) {
        Triple(
                receiverState ?: serviceController.receiverState.value,
                selectedService ?: serviceController.selectedService.value,
                playbackState ?: viewController?.rmpState?.value
        )
    }

    private class ViewPresentationLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)

        fun start() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        fun stopAndDestroy() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        override fun getLifecycle() = registry
    }

    class InitializationException : RuntimeException()

    companion object {
        val TAG: String = Atsc3ForegroundService::class.java.simpleName

        private const val PRESENTATION_DESTROYING_DELAY = 1000L

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