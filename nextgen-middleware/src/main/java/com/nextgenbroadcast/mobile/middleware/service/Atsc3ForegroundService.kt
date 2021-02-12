package com.nextgenbroadcast.mobile.middleware.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.media.session.MediaButtonReceiver
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.middleware.BuildConfig
import com.nextgenbroadcast.mobile.middleware.atsc3.source.UsbAtsc3Source
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.phy.Atsc3DeviceReceiver
import com.nextgenbroadcast.mobile.middleware.atsc3.core.Atsc3ReceiverCore
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.SLTConstants
import com.nextgenbroadcast.mobile.middleware.atsc3.player.Atsc3MediaPlayer
import com.nextgenbroadcast.mobile.middleware.cache.DownloadManager
import com.nextgenbroadcast.mobile.middleware.service.init.*
import kotlinx.coroutines.*

abstract class Atsc3ForegroundService : BindableForegroundService() {
    private val usbManager: UsbManager by lazy {
        getSystemService(Context.USB_SERVICE) as UsbManager
    }

    private val state = MediatorLiveData<Triple<ReceiverState?, AVService?, PlaybackState?>>()
    private val playerState = MutableLiveData<PlaybackState>()
    private val playbackState = MediatorLiveData<PlaybackState>()

    // Receiver Core
    private lateinit var atsc3Receiver: Atsc3ReceiverCore
    private lateinit var wakeLock: WakeLock

    // Media Service
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private lateinit var player: Atsc3MediaPlayer

    // View Presentation
    private var viewPresentationLifecycle: ViewPresentationLifecycleOwner? = null
    private var deviceReceiver: Atsc3DeviceReceiver? = null
    private var destroyPresentationLayerJob: Job? = null

    private var isInitialized = false

    override fun onCreate() {
        super.onCreate()

        createReceiverCore()
        createMediaSession()

        startStateObservation()
    }

    private fun createReceiverCore() {
        atsc3Receiver = Atsc3ReceiverCore.getInstance(applicationContext)
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Atsc3ForegroundService::lock")
    }

    private fun createMediaSession() {
        stateBuilder = PlaybackStateCompat.Builder()
        mediaSession = MediaSessionCompat(applicationContext, TAG).apply {
            setPlaybackState(stateBuilder.build())
            setCallback(MediaSessionCallback())
        }.also { session ->
            sessionToken = session.sessionToken
        }

        player = Atsc3MediaPlayer(applicationContext).apply {
            setListener(object : Atsc3MediaPlayer.EventListener {
                override fun onPlayerStateChanged(state: PlaybackState) {
                    playerState.value = state
                }

                override fun onPlayerError(error: Exception) {
                    Log.d(TAG, error.message ?: "")
                }

                override fun onPlaybackSpeedChanged(speed: Float) {
                    atsc3Receiver.viewController?.rmpPlaybackRateChanged(speed)
                }
            })
        }
    }

    private fun startStateObservation() {
        playbackState.apply {
            addSource(playerState) { state ->
                playbackState.value = mergePlaybackState(atsc3Receiver.viewController?.rmpState?.value, state)
            }
        }.observe(this) { state ->
            if (state == PlaybackState.PLAYING) {
                setPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            } else {
                setPlaybackState(PlaybackStateCompat.STATE_PAUSED)
            }
        }

        state.apply {
            addSource(atsc3Receiver.serviceController.receiverState) { receiverState ->
                value = newState(receiverState = receiverState)
            }
            addSource(atsc3Receiver.serviceController.selectedService) { service ->
                value = newState(selectedService = service)
            }
            addSource(playbackState) { playbackState ->
                value = newState(playbackState = playbackState)
            }
        }.observe(this, { (receiverState, selectedService, playbackState) ->
            if (isForeground) {
                pushNotification(createNotificationBuilder(receiverState, selectedService, playbackState, mediaSession))
            }
        })

        with(atsc3Receiver) {
            serviceController.sltServices.distinctUntilChanged().observe(this@Atsc3ForegroundService) { services ->
                val queue = services.map { service ->
                    MediaSessionCompat.QueueItem(
                            MediaDescriptionCompat.Builder()
                                    .setMediaId(service.globalId)
                                    .setTitle(service.shortName)
                                    .setExtras(service.toBundle())
                                    .build(),
                            service.id.toLong()
                    )
                }
                mediaSession.setQueue(queue)

                // Automatically start playing the first service in list
                if (playbackState.value == PlaybackState.IDLE) {
                    services?.firstOrNull()?.let { service ->
                        selectMediaService(service)
                    }
                }
            }
            serviceController.selectedService.observe(this@Atsc3ForegroundService) { service ->
                mediaSession.setQueueTitle(service?.shortName)
            }
            serviceController.routeMediaUrl.distinctUntilChanged().observe(this@Atsc3ForegroundService) { mediaPath ->
                val service = mediaPath?.let {
                    findServiceBy(mediaPath.bsid, mediaPath.serviceId)
                }
                if (!isBinded || (ignoreAudioServiceMedia && service?.category == SLTConstants.SERVICE_CATEGORY_AO)) {
                    mediaPath?.let {
                        player.play(mediaFileProvider.getMediaFileUri(mediaPath.url))
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        player.reset()

        destroyViewPresentationAndStopService()

        mediaSession.release()

        atsc3Receiver.deInitialize()
    }

    override fun onBind(intent: Intent): IBinder? {
        if (intent.action == SERVICE_INTERFACE) {
            tryStopPlaybackOnBoard()

            val playAudioOnBoard = intent.getBooleanExtra(EXTRA_PLAY_AUDIO_ON_BOARD, true)

            cancelPresentationDestroying()
            createViewPresentationAndStartService(playAudioOnBoard)

            maybeInitialize()

            return createServiceBinder(atsc3Receiver.serviceController)
        }

        return super.onBind(intent)
    }

    abstract fun createServiceBinder(serviceController: IServiceController): IBinder

    override fun onRebind(intent: Intent) {
        if (intent.action == SERVICE_INTERFACE) {
            tryStopPlaybackOnBoard()

            cancelPresentationDestroying()
        }

        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        if (intent.action == SERVICE_INTERFACE) {
            tryRestorePlaybackOnBoard()

            cancelPresentationDestroying()
            if (isStartedAsForeground) {
                startPresentationDestroying()
            } else {
                destroyViewPresentationAndStopService()
            }
        }

        return super.onUnbind(intent)
    }

    private fun tryStopPlaybackOnBoard() {
        atsc3Receiver.getCurrentlyPlayingMediaUrl()?.let { mediaUrl ->
            val service = atsc3Receiver.findServiceBy(mediaUrl.bsid, mediaUrl.serviceId)
            if (service?.category != SLTConstants.SERVICE_CATEGORY_AO) {
                player.reset()
            }
        } ?: player.reset()
    }

    private fun tryRestorePlaybackOnBoard() {
        val currentMediaUri = atsc3Receiver.getCurrentlyPlayingMediaUri()
        if (currentMediaUri != null && !player.isPlaying) {
            player.play(currentMediaUri)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        MediaButtonReceiver.handleIntent(mediaSession, intent)

        if (intent != null) {
            when (intent.action) {
                ACTION_DEVICE_ATTACHED -> onDeviceAttached(intent.getParcelableExtra(UsbManager.EXTRA_DEVICE))

                ACTION_DEVICE_DETACHED -> onDeviceDetached(intent.getParcelableExtra(UsbManager.EXTRA_DEVICE))

                ACTION_USB_PERMISSION -> intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false).let { granted ->
                    if (granted) {
                        onDevicePermissionGranted(intent.getParcelableExtra(UsbManager.EXTRA_DEVICE))
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
            BrowserRoot(MEDIA_ROOT_ID, null)
        } else {
            null
        }
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()

        if (parentId == MEDIA_ROOT_ID) {
            sourceList.forEach { (title, path, id) ->
                mediaItems.add(MediaBrowserCompat.MediaItem(
                        MediaDescriptionCompat.Builder()
                                .setMediaId(id)
                                .setTitle(title)
                                .setMediaUri(Uri.parse(path))
                                .build(),
                        MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                ))
            }
        }

        result.sendResult(mediaItems)
    }

    private fun maybeInitialize() {
        if (isInitialized) return
        isInitialized = true

        try {
            atsc3Receiver.initialize(MetadataReader.discoverMetadata(this))
        } catch (e: Exception) {
            Log.d(TAG, "Can't initialize, something is wrong in metadata", e)
        }
    }

    private fun openRoute(filePath: String?) {
        // change source to file. So, let's unregister device receiver
        unregisterDeviceReceiver()

        filePath?.let {
            atsc3Receiver.openRoute(filePath)
        }
    }

    private fun openRoute(device: UsbDevice) {
        startForeground()
        unregisterDeviceReceiver()

        atsc3Receiver.openRoute(UsbAtsc3Source(usbManager, device))

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
        destroyViewPresentationAndStopService()
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

    private fun registerDeviceReceiver(device: UsbDevice) {
        deviceReceiver = Atsc3DeviceReceiver(device.deviceName).also { receiver ->
            registerReceiver(receiver, IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            })
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

    override fun getReceiverState() = atsc3Receiver.getReceiverState()

    private fun createViewPresentationAndStartService(ignoreAudioServiceMedia: Boolean) {
        // we release it only when destroy presentation layer
        if (wakeLock.isHeld) return

        viewPresentationLifecycle?.stopAndDestroy()
        val viewLifecycle = ViewPresentationLifecycleOwner().apply {
            start()
        }.also {
            viewPresentationLifecycle = it
        }

        val downloadManager = DownloadManager()
        val view = atsc3Receiver.createAndStartViewPresentation(downloadManager, viewLifecycle, ignoreAudioServiceMedia)

        playbackState.addSource(view.rmpState) { state ->
            playbackState.value = mergePlaybackState(state, playerState.value)
        }

        //TODO: add lock limitation??
        wakeLock.acquire()
    }

    private fun destroyViewPresentationAndStopService() {
        viewPresentationLifecycle?.stopAndDestroy()
        viewPresentationLifecycle = null

        atsc3Receiver.viewController?.let { view ->
            playbackState.removeSource(view.rmpState)
        }

        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        atsc3Receiver.stopAndDestroyViewPresentation()
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
        if (atsc3Receiver.viewController == null) {
            createViewPresentationAndStartService(atsc3Receiver.ignoreAudioServiceMedia)
        }
        return atsc3Receiver.viewController ?: throw InitializationException()
    }

    private fun mergePlaybackState(firstState: PlaybackState?, secondState: PlaybackState?): PlaybackState {
        return if (firstState == PlaybackState.PLAYING || secondState == PlaybackState.PLAYING) {
            PlaybackState.PLAYING
        } else if (firstState == PlaybackState.PAUSED || secondState == PlaybackState.PAUSED) {
            PlaybackState.PAUSED
        } else {
            PlaybackState.IDLE
        }
    }

    private fun newState(receiverState: ReceiverState? = null, selectedService: AVService? = null, playbackState: PlaybackState? = null) = with(atsc3Receiver) {
        Triple(
                receiverState ?: serviceController.receiverState.value,
                selectedService ?: serviceController.selectedService.value,
                playbackState ?: this@Atsc3ForegroundService.playbackState.value
        )
    }

    private fun setPlaybackState(@PlaybackStateCompat.State state: Int) {
        val playbackState = if (state == PlaybackStateCompat.STATE_PLAYING) {
            PlaybackStateCompat.ACTION_PAUSE
        } else {
            PlaybackStateCompat.ACTION_PLAY
        }
        stateBuilder
                .setActions(
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                or PlaybackStateCompat.ACTION_PLAY_PAUSE
                                or playbackState
                )
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f)
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    private fun successfullyRetrievedAudioFocus(): Boolean {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                .build()
        val result = audioManager.requestAudioFocus(request)
        return result == AudioManager.AUDIOFOCUS_GAIN
    }

    private fun selectMediaService(service: AVService) {
        val result = atsc3Receiver.serviceController.selectService(service)
        if (result) {
            player.reset()

            mediaSession.setMetadata(MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, service.shortName)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "${service.majorChannelNo}-${service.minorChannelNo}")
                    .build())

            //TODO: we must deactivate it
            mediaSession.isActive = true
        }
    }

    inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            if (!player.playWhenReady) {
                player.playWhenReady = true
            } else {
                atsc3Receiver.viewController?.rmpResume()
            }
        }

        override fun onPause() {
            if (player.isPlaying) {
                player.playWhenReady = false
            } else {
                atsc3Receiver.viewController?.rmpPause()
            }
        }

        override fun onSkipToNext() {
            atsc3Receiver.getNextService()?.let { service ->
                selectMediaService(service)
            }
        }

        override fun onSkipToPrevious() {
            atsc3Receiver.getPreviousService()?.let { service ->
                selectMediaService(service)
            }
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            if (!successfullyRetrievedAudioFocus()) return

            val globalId = mediaId ?: return
            atsc3Receiver.serviceController.findServiceById(globalId)?.let { service ->
                selectMediaService(service)
            }
        }
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

        const val SERVICE_INTERFACE = "${BuildConfig.LIBRARY_PACKAGE_NAME}.INTERFACE"

        private const val SERVICE_ACTION = "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action"

        const val ACTION_DEVICE_ATTACHED = "$SERVICE_ACTION.USB_ATTACHED"
        const val ACTION_DEVICE_DETACHED = "$SERVICE_ACTION.USB_DETACHED"
        const val ACTION_USB_PERMISSION = "$SERVICE_ACTION.USB_PERMISSION"
        const val ACTION_RMP_PLAY = "$SERVICE_ACTION.RMP_PLAY"
        const val ACTION_RMP_PAUSE = "$SERVICE_ACTION.RMP_PAUSE"
        const val ACTION_OPEN_ROUTE = "$SERVICE_ACTION.OPEN_ROUTE"
        const val ACTION_CLOSE_ROUTE = "$SERVICE_ACTION.CLOSE_ROUTE"

        const val EXTRA_DEVICE = UsbManager.EXTRA_DEVICE
        const val EXTRA_ROUTE_PATH = "route_path"
        const val EXTRA_PLAY_AUDIO_ON_BOARD = "play_audio_on_board"

        const val MEDIA_ROOT_ID = "2262d068-67cf-11eb-ae93-0242ac130002"

        val sourceList = listOf(
                Triple("las", "srt://las.srt.atsc3.com:31350?passphrase=A166AC45-DB7C-4B68-B957-09B8452C76A4", "A166AC45-DB7C-4B68-B957-09B8452C76A4"),
                Triple("bna", "srt://bna.srt.atsc3.com:31347?passphrase=88731837-0EB5-4951-83AA-F515B3BEBC20", "88731837-0EB5-4951-83AA-F515B3BEBC20"),
                Triple("slc", "srt://slc.srt.atsc3.com:31341?passphrase=B9E4F7B8-3CDD-4BA2-ACA6-13088AB855C0", "B9E4F7B8-3CDD-4BA2-ACA6-13088AB855C0"),
                Triple("lab", "srt://lab.srt.atsc3.com:31340?passphrase=03760631-667B-4ADB-9E04-E4491B0A7CF1", "03760631-667B-4ADB-9E04-E4491B0A7CF1"),
                Triple("qa", "srt://lab.srt.atsc3.com:31347?passphrase=f51e5a22-9b73-4ec8-be84-e4c173f1d913", "f51e5a22-9b73-4ec8-be84-e4c173f1d913"),
                Triple("labJJ", "srt://lab.srt.atsc3.com:31346?passphrase=055E0771-97B2-4447-8B5C-3B2497D0DE32", "055E0771-97B2-4447-8B5C-3B2497D0DE32"),
                Triple("labJJPixel5", "srt://lab.srt.atsc3.com:31348?passphrase=3D5E5ED2-700D-443B-968F-598DB9A2750D&packetfilter=fec", "3D5E5ED2-700D-443B-968F-598DB9A2750D"),
                Triple("seaJJAndroid", "srt://sea.srt.atsc3.com:31346?passphrase=055E0771-97B2-4447-8B5C-3B2497D0DE32", "055E0771-97B2-4447-8B5C-3B2497D0DE32")
        )

        internal lateinit var clazz: Class<out Atsc3ForegroundService>

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