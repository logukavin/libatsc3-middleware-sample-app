package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextgenbroadcast.mobile.middleware.scoreboard.ScoreboardPagerActivity.Companion.SELECTED_DEVICE_ID
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TelemetryDevice
import com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry.DatagramSocketWrapper
import com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry.TelemetryManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class ForegroundService : IDeviceSelectionListener, Service() {
    private var selectedDeviceId: String? = null
    private var currentDeviceIds: List<TelemetryDevice>? = null
    private var currentDeviceSelection: String? = null
    private var job: Job? = null
    private val gson = Gson()
    private val phyType = object : TypeToken<ScoreboardFragment.PhyPayload>() {}.type
    private lateinit var telemetryManager: TelemetryManager

    private lateinit var binder: ForegroundBinding
    private val socket: DatagramSocketWrapper by lazy {
        DatagramSocketWrapper(applicationContext)
    }

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
            val broadcastIntent = Intent(ScoreboardPagerActivity.ACTION_DEVICE_IDS)
            broadcastIntent.putExtra(ScoreboardPagerActivity.DEVICE_IDS, deviceIds.toTypedArray())
            broadcastIntent.putExtra(SELECTED_DEVICE_ID, selectedDeviceId)
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
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

    override fun deviceSelectionEvent(deviceSelection: String?) {
        currentDeviceSelection = deviceSelection
        job?.cancel("Device connection changed", null)
        selectedDeviceId = null
        deviceSelection?.let { id ->
            selectedDeviceId = id
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

        if (this::binder.isInitialized) {
            return binder
        } else {
            deviceId?.let { id ->
                telemetryManager = TelemetryManager(this, id) { deviceIds ->
                    currentDeviceIds = deviceIds
                    val broadcastIntent = Intent(ScoreboardPagerActivity.ACTION_DEVICE_IDS)
                    broadcastIntent.putExtra(ScoreboardPagerActivity.DEVICE_IDS, deviceIds.toTypedArray())
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
                }.also { telemetryManager ->
                    binder = ForegroundBinding(telemetryManager, this, currentDeviceIds)
                    telemetryManager.start()
                }
            }
            return binder
        }
    }


    class ForegroundBinding(
        val telemetryManager: TelemetryManager?,
        private val deviceSelectListener: IDeviceSelectionListener,
        val currentDeviceIds: List<TelemetryDevice>?
    ) : Binder() {

        fun changeDeviceSelection(deviceSelection: String?) {
            deviceSelectListener.deviceSelectionEvent(deviceSelection)
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
    fun deviceSelectionEvent(deviceSelection: String?)
}


