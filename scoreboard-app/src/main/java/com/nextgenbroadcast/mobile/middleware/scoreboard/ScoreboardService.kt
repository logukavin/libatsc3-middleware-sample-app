package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TelemetryDevice
import com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry.DatagramSocketWrapper
import com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry.TelemetryManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ScoreboardService : IDeviceSelectionListener, Service() {
    private val gson = Gson()
    private val phyType = object : TypeToken<ScoreboardFragment.PhyPayload>() {}.type
    private val socket: DatagramSocketWrapper by lazy {
        DatagramSocketWrapper(applicationContext)
    }

    private var currentDeviceIds: List<TelemetryDevice>? = null
    private var currentDeviceSelection: String? = null
    private var job: Job? = null
    private lateinit var binder: ScoreboardBinding
    private lateinit var telemetryManager: TelemetryManager

    override fun onCreate() {
        super.onCreate()
        val pendingIntent: PendingIntent =
            Intent(this, ScoreboardPagerActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
            }

        createNotificationChannel()
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.app_name))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentDeviceIds?.let { deviceIds ->
            binder.updateDeviceIds(deviceIds)
            binder.updateSelectedDeviceId(currentDeviceSelection)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID,
            CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
    }

    override fun selectDevice(deviceId: String?) {
        currentDeviceSelection = deviceId
        job?.cancel("Device connection changed", null)

        deviceId?.let { id ->

            job = CoroutineScope(Dispatchers.IO).launch {
                telemetryManager.getFlow(id)?.collect { event ->
                    try {
                        val payload =
                            gson.fromJson<ScoreboardFragment.PhyPayload>(event.payload, phyType)
                        val payloadValue = payload.snr1000.toDouble() / 1000
                        socket.sendUdpMessage("${id},${payload.timeStamp},$payloadValue")
                    } catch (e: Exception) {
                        Log.w(ScoreboardFragment.TAG, "Can't parse telemetry event payload", e)
                    }
                }
            }
        }
    }


    override fun onBind(intent: Intent): IBinder {
        val deviceId = intent.extras?.getString(DEVICE_ID)

        deviceId?.let { id ->
            telemetryManager = TelemetryManager(this, id) { deviceIds ->
                currentDeviceIds = deviceIds
                binder.updateDeviceIds(deviceIds)
            }.also { telemetryManager ->
                binder = ScoreboardBinding(telemetryManager, this, currentDeviceIds)
                telemetryManager.start()
            }
        }

        return binder
    }


    class ScoreboardBinding(
        val telemetryManager: TelemetryManager?,
        val deviceSelectListener: IDeviceSelectionListener,
        val currentDeviceIds: List<TelemetryDevice>?
    ) : Binder() {
        private val _deviceIds: MutableSharedFlow<List<TelemetryDevice>> = MutableSharedFlow()
        val deviceIds: SharedFlow<List<TelemetryDevice>> = _deviceIds.asSharedFlow()

        private val _selectedDeviceFlow: MutableSharedFlow<String?> = MutableSharedFlow()
        val selectedDeviceFlow: SharedFlow<String?> = _selectedDeviceFlow.asSharedFlow()

        val coroutineScope = CoroutineScope(Dispatchers.Main)

        fun updateDeviceIds(deviceIdsList: List<TelemetryDevice>) {
            coroutineScope.launch {
                _deviceIds.emit(deviceIdsList)
            }
        }

        fun updateSelectedDeviceId(selectedDeviceId: String?) {
            coroutineScope.launch {
                _selectedDeviceFlow.emit(selectedDeviceId)
            }
        }
    }

    companion object {
        const val DEVICE_ID = "device_id"
        private const val CHANNEL_ID: String = "foreground_phy"
        private const val CHANNEL_NAME: String = "foreground_phy_name"
        private const val NOTIFICATION_ID = 34569
    }

}

interface IDeviceSelectionListener {
    fun selectDevice(deviceId: String?)
}


