package com.nextgenbroadcast.mobile.middleware.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.*
import android.os.*
import android.os.PowerManager.WakeLock
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.core.content.ContextCompat
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.middleware.*
import com.nextgenbroadcast.mobile.middleware.atsc3.source.Atsc3Source
import com.nextgenbroadcast.mobile.middleware.atsc3.source.UsbAtsc3Source
import com.nextgenbroadcast.mobile.middleware.cache.DownloadManager
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.notification.AlertNotificationHelper
import com.nextgenbroadcast.mobile.middleware.phy.Atsc3DeviceReceiver
import com.nextgenbroadcast.mobile.middleware.service.holder.*
import com.nextgenbroadcast.mobile.middleware.service.init.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

abstract class Atsc3ForegroundService : BindableForegroundService() {
    private val usbManager: UsbManager by lazy {
        getSystemService(Context.USB_SERVICE) as UsbManager
    }
    private val powerManager: PowerManager by lazy {
        getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    //TODO: create own scope?
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    private val viewPlayerState = MutableStateFlow(PlaybackState.IDLE)

    private lateinit var state: StateFlow<Triple<ReceiverState?, AVService?, PlaybackState?>>
    private lateinit var playbackState: StateFlow<PlaybackState>

    // Receiver Core
    private lateinit var atsc3Receiver: Atsc3ReceiverCore
    private lateinit var webServer: WebServerHolder
    private lateinit var wakeLock: WakeLock

    // Media Service
    private lateinit var media: MediaHolder

    // View Presentation
    private var deviceReceiver: Atsc3DeviceReceiver? = null
    private var destroyPresentationLayerJob: Job? = null

    // Telemetry
    internal lateinit var telemetryHolder: TelemetryHolder

    // Srt
    private lateinit var srtList: SrtListHolder

    // Location
    private lateinit var locationHolder: LocationHolder

    // Initialization from Service metadata
    private val initializer = ArrayList<WeakReference<IServiceInitializer>>()
    private var isInitialized = false

    private val alertNotificationHelper by lazy {
        AlertNotificationHelper(this)
    }

    override fun onCreate() {
        super.onCreate()

        atsc3Receiver = Atsc3ReceiverStandalone.get(applicationContext)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Atsc3ForegroundService::lock")

        locationHolder = LocationHolder(applicationContext, atsc3Receiver)

        media = MediaHolder(applicationContext, atsc3Receiver).also {
            it.open()
            sessionToken = it.sessionToken
        }

        telemetryHolder = TelemetryHolder(applicationContext, atsc3Receiver).also {
            it.open()
        }

        webServer = WebServerHolder(applicationContext, atsc3Receiver,
                { server ->
                    // used to rebuild data related to server
                    atsc3Receiver.notifyNewSessionStarted()

                    telemetryHolder.notifyWebServerStarted(server)
                },
                {
                    telemetryHolder.notifyWebServerStopped()
                }
        )

        srtList = SrtListHolder(applicationContext).apply {
            open()
        }

        startStateObservation()
    }

    private fun startStateObservation() {
        playbackState = combine(media.embeddedPlayerState, viewPlayerState) { firstState, secondState ->
            if (firstState == PlaybackState.PLAYING || secondState == PlaybackState.PLAYING) {
                PlaybackState.PLAYING
            } else if (firstState == PlaybackState.PAUSED || secondState == PlaybackState.PAUSED) {
                PlaybackState.PAUSED
            } else {
                PlaybackState.IDLE
            }
        }.stateIn(serviceScope, SharingStarted.Eagerly, PlaybackState.IDLE)

        locationHolder.open(serviceScope)

        serviceScope.launch {
            playbackState.collect { state ->
                withContext(Dispatchers.Main) {
                    media.setPlaybackState(state)
                }
            }
        }

        state = combine(atsc3Receiver.serviceController.receiverState, atsc3Receiver.serviceController.selectedService, playbackState) { receiverState, selectedService, playbackState ->
            Triple(receiverState, selectedService, playbackState)
        }.stateIn(serviceScope, SharingStarted.Eagerly, Triple(ReceiverState.idle(), null, PlaybackState.IDLE))

        serviceScope.launch {
            state.collect { (receiverState, selectedService, playbackState) ->
                withContext(Dispatchers.Main) {
                    if (isForeground) {
                        pushNotification(createNotificationBuilder(receiverState, selectedService, playbackState))
                    }
                }
            }
        }

        serviceScope.launch {
            atsc3Receiver.serviceController.routeServices.collect { services ->
                withContext(Dispatchers.Main) {
                    media.setQueue(services)
                }
            }
        }

        serviceScope.launch {
            atsc3Receiver.serviceController.selectedService.collect { service ->
                withContext(Dispatchers.Main) {
                    media.setQueueSelection(service)
                }
            }
        }

        serviceScope.launch {
            atsc3Receiver.repository.routeMediaUrl.collect { mediaPath ->
                withContext(Dispatchers.Main) {
                    media.startPlaybackIfServicerAvailable(mediaPath)
                }
            }
        }

        serviceScope.launch {
            atsc3Receiver.serviceController.alertList.collect { alerts ->
                val locale = Locale.getDefault().language

                alerts.forEach { alert ->
                    val message = alert.messages?.get(locale) ?: alert.messages?.get(
                        alert.messages?.keys?.first()
                    )

                    message?.let { msg ->
                        alertNotificationHelper.showNotification(msg, alert.id, alert.effective)
                        startActivity(
                            AlertDialogActivity.newIntent(
                                this@Atsc3ForegroundService, msg, alert.effective, alert.id
                            )
                        )
                    }

                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        initializer.forEach { ref ->
            ref.get()?.cancel()
        }

        destroyViewPresentation()

        media.close()
        webServer.close()
        atsc3Receiver.deInitialize()
        serviceScope.cancel()
        telemetryHolder.close()
    }

    internal abstract fun createServiceBinder(serviceController: IServiceController): IBinder

    override fun onBind(intent: Intent): IBinder? {
        if (intent.action == SERVICE_INTERFACE) {
            media.stopPlaybackIfInitialized()

            val playAudioOnBoard = intent.getBooleanExtra(EXTRA_PLAY_AUDIO_ON_BOARD, true)

            cancelViewPresentationDestroying()
            createViewPresentationAndStartService(playAudioOnBoard)

            maybeInitialize()

            return createServiceBinder(atsc3Receiver.serviceController)
        }

        return super.onBind(intent)
    }

    override fun onRebind(intent: Intent) {
        if (intent.action == SERVICE_INTERFACE) {
            media.stopPlaybackIfInitialized()

            cancelViewPresentationDestroying()
            webServer.open()
        }

        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        if (intent.action == SERVICE_INTERFACE) {
            cancelViewPresentationDestroying()
            if (isStartedAsForeground) {
                destroyViewPresentationDelayed()
            } else {
                destroyViewPresentation()
            }
        }

        super.onUnbind(intent)

        return true // allow reBind
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        media.handleIntent(intent)

        if (intent != null) {
            when (intent.action) {
                ACTION_START_FOREGROUND -> startForeground()

                ACTION_DEVICE_ATTACHED -> onDeviceAttached(
                        intent.getParcelableExtra(EXTRA_DEVICE),
                        intent.getIntExtra(EXTRA_DEVICE_TYPE, Atsc3Source.DEVICE_TYPE_AUTO),
                        intent.getBooleanExtra(EXTRA_FORCE_OPEN, true)
                )

                ACTION_DEVICE_DETACHED -> onDeviceDetached(intent.getParcelableExtra(EXTRA_DEVICE))

                ACTION_USB_PERMISSION -> intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false).let { granted ->
                    if (granted) {
                        onDevicePermissionGranted(
                                intent.getParcelableExtra(EXTRA_DEVICE),
                                intent.getIntExtra(EXTRA_DEVICE_TYPE, Atsc3Source.DEVICE_TYPE_AUTO),
                                intent.getBooleanExtra(EXTRA_FORCE_OPEN, true)
                        )
                    }
                }

                ACTION_RMP_PLAY -> atsc3Receiver.viewController?.rmpResume()

                ACTION_RMP_PAUSE -> atsc3Receiver.viewController?.rmpPause()

                ACTION_OPEN_ROUTE -> openRoute(intent.getStringExtra(EXTRA_ROUTE_PATH))

                ACTION_CLOSE_ROUTE -> closeRoute()

                else -> {
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return if (clientPackageName == packageName) {
            MediaHolder.getRoot()
        } else {
            null
        }
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        if (MediaHolder.isRoot(parentId)) {
            result.sendResult(srtList.getFullSrtList().map { (title, path, id) ->
                MediaHolder.getItem(title, path, id)
            })
            return
        }

        result.sendResult(emptyList())
    }

    private fun maybeInitialize() {
        if (isInitialized) return
        isInitialized = true

        val appContext = applicationContext
        val components = MetadataReader.discoverMetadata(this)

        val handler = CoroutineExceptionHandler { _, e ->
            Log.d(TAG, "Can't initialize, something is wrong in metadata", e)
        }

        try {
            serviceScope.launch(handler) {
                FrequencyInitializer(atsc3Receiver.settings, atsc3Receiver).also {
                    initializer.add(WeakReference(it))
                }.initialize(appContext, components)
            }

            // Do not re-open the libatsc3 if it's already opened
            if (!atsc3Receiver.isIdle()) return

            val phyInitializer = OnboardPhyInitializer(atsc3Receiver).also {
                initializer.add(WeakReference(it))
            }

            serviceScope.launch(handler) {
                if (phyInitializer.initialize(appContext, components)) {
                    startForeground(applicationContext)
                } else {
                    UsbPhyInitializer().also {
                        initializer.add(WeakReference(it))
                    }.initialize(appContext, components)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Can't initialize, something is wrong in metadata", e)
        }

        val sourcePath = srtList.getDefaultRoute()
        if (sourcePath != null) {
            openRoute(applicationContext, sourcePath)
        }
    }

    private fun openRoute(sourcePath: String?) {
        // change source to file. So, let's unregister device receiver
        unregisterDeviceReceiver()

        sourcePath?.let {
            routePathToSource(applicationContext, sourcePath)?.let { source ->
                atsc3Receiver.openRoute(source, true)
            }
        }
    }

    private fun openRoute(device: UsbDevice, deviceType: Int, forceOpen: Boolean) {
        startForeground()
        unregisterDeviceReceiver()

        atsc3Receiver.openRoute(UsbAtsc3Source(usbManager, device, deviceType), forceOpen)

        // Register BroadcastReceiver to detect when device is disconnected
        registerDeviceReceiver(device)
    }

    private fun closeRoute() {
        unregisterDeviceReceiver()

        atsc3Receiver.closeRoute()

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
        destroyViewPresentation()
        stopForeground()
        stopSelf()
    }

    private fun onDeviceAttached(device: UsbDevice?, deviceType: Int, forceOpen: Boolean) {
        if (device == null) {
            if (!isForeground && !isBinded) {
                stopSelf()
            }
            return
        }

        //TODO: process case with second connected device
        if (usbManager.hasPermission(device)) {
            openRoute(device, deviceType, forceOpen)
        } else {
            requestDevicePermission(device, deviceType, forceOpen)
        }
    }

    private fun onDeviceDetached(device: UsbDevice?) {
        closeRoute()
    }

    private fun onDevicePermissionGranted(device: UsbDevice?, deviceType: Int, forceOpen: Boolean) {
        device?.let {
            // open device using a new Intent to start Service as foreground
            startForDevice(this, device, deviceType, forceOpen)
        }
    }

    private fun registerDeviceReceiver(device: UsbDevice) {
        deviceReceiver = Atsc3DeviceReceiver(device.deviceName) {
            stopForDevice(applicationContext, device)
        }.also { receiver ->
            registerReceiver(receiver, receiver.intentFilter)
        }
    }

    private fun unregisterDeviceReceiver() {
        deviceReceiver?.let { receiver ->
            unregisterReceiver(receiver)
            deviceReceiver = null
        }
    }

    private fun requestDevicePermission(device: UsbDevice, deviceType: Int, forceOpen: Boolean) {
        val intent = Intent(this, clazz).apply {
            action = ACTION_USB_PERMISSION
            putExtra(EXTRA_DEVICE_TYPE, deviceType)
            putExtra(EXTRA_FORCE_OPEN, forceOpen)
        }
        usbManager.requestPermission(device, PendingIntent.getService(this, 0, intent, 0))
    }

    override fun getReceiverState() = atsc3Receiver.getReceiverState()

    private fun createViewPresentationAndStartService(ignoreAudioServiceMedia: Boolean) {
        // we release it only when destroy presentation layer
        if (wakeLock.isHeld) return

        val downloadManager = DownloadManager()
        atsc3Receiver.createViewPresentation(downloadManager, ignoreAudioServiceMedia) { view, viewScope ->
            viewScope.launch {
                view.rmpState.onCompletion {
                    viewPlayerState.value = PlaybackState.IDLE
                }.collect { state ->
                    viewPlayerState.value = state
                }
            }
        }

        webServer.open()

        //TODO: add lock limitation??
        wakeLock.acquire()
    }

    private fun destroyViewPresentation() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        // Don't really destroy View Presentation because it could be pointed by Binder and re-binded
        //atsc3Receiver.stopAndDestroyViewPresentation()
        webServer.close()
    }

    private fun destroyViewPresentationDelayed() {
        destroyPresentationLayerJob = CoroutineScope(Dispatchers.IO).launch {
            delay(PRESENTATION_DESTROYING_DELAY)
            withContext(Dispatchers.Main) {
                destroyViewPresentation()
                destroyPresentationLayerJob = null
            }
        }
    }

    private fun cancelViewPresentationDestroying() {
        destroyPresentationLayerJob?.let {
            it.cancel()
            destroyPresentationLayerJob = null
        }
    }

    internal fun requireViewController(): IViewController {
        if (atsc3Receiver.viewController == null) {
            createViewPresentationAndStartService(atsc3Receiver.ignoreAudioServiceMedia)
        }
        return atsc3Receiver.viewController ?: throw InitializationException()
    }

    class InitializationException : RuntimeException()

    companion object {
        val TAG: String = Atsc3ForegroundService::class.java.simpleName

        private const val PRESENTATION_DESTROYING_DELAY = 1000L

        const val SERVICE_INTERFACE = "${BuildConfig.LIBRARY_PACKAGE_NAME}.INTERFACE"

        private const val SERVICE_ACTION = "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action"

        internal const val ACTION_START_FOREGROUND = "$SERVICE_ACTION.START_FOREGROUND"

        const val ACTION_DEVICE_ATTACHED = "$SERVICE_ACTION.USB_ATTACHED"
        const val ACTION_DEVICE_DETACHED = "$SERVICE_ACTION.USB_DETACHED"
        const val ACTION_USB_PERMISSION = "$SERVICE_ACTION.USB_PERMISSION"
        const val ACTION_RMP_PLAY = "$SERVICE_ACTION.RMP_PLAY"
        const val ACTION_RMP_PAUSE = "$SERVICE_ACTION.RMP_PAUSE"
        const val ACTION_OPEN_ROUTE = "$SERVICE_ACTION.OPEN_ROUTE"
        const val ACTION_CLOSE_ROUTE = "$SERVICE_ACTION.CLOSE_ROUTE"

        const val EXTRA_DEVICE = UsbManager.EXTRA_DEVICE
        const val EXTRA_DEVICE_TYPE = "device_type"
        const val EXTRA_ROUTE_PATH = "route_path"
        const val EXTRA_PLAY_AUDIO_ON_BOARD = "play_audio_on_board"
        const val EXTRA_FORCE_OPEN = "force_open"

        internal lateinit var clazz: Class<out Atsc3ForegroundService>

        internal fun startForeground(context: Context) {
            newIntent(context, ACTION_START_FOREGROUND).let { serviceIntent ->
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }

        fun startForDevice(context: Context, device: UsbDevice, deviceType: Int, forceOpen: Boolean = true) {
            newIntent(context, ACTION_DEVICE_ATTACHED).let { serviceIntent ->
                serviceIntent.putExtra(EXTRA_DEVICE, device)
                serviceIntent.putExtra(EXTRA_DEVICE_TYPE, deviceType)
                serviceIntent.putExtra(EXTRA_FORCE_OPEN, forceOpen)
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