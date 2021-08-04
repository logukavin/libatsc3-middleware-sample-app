package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.google.firebase.installations.FirebaseInstallations
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.ClientTelemetryEvent
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TelemetryDevice
import com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry.DatagramSocketWrapper
import com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry.TelemetryManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ScoreboardService : Service() {
    private val gson = Gson()
    private val phyType = object : TypeToken<ScoreboardFragment.PhyPayload>() {}.type
    private val deviceIds = MutableStateFlow<List<TelemetryDevice>>(emptyList())
    private val selectedDeviceId = MutableStateFlow<String?>(null)
    private val socket: DatagramSocketWrapper by lazy {
        DatagramSocketWrapper(applicationContext)
    }

    private lateinit var telemetryManager: TelemetryManager

    private var socketJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                    task.result?.let { deviceId ->
                        telemetryManager = TelemetryManager(applicationContext, deviceId) { list ->
                            deviceIds.value = list
                        }.also {
                            it.start()
                        }
                    }
            } else {
                LOG.e(TAG, "Can't create Telemetry because Firebase ID not received.", task.exception)
            }
        }

        val pendingIntent = Intent(this, ScoreboardPagerActivity::class.java).let { notificationIntent ->
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

    override fun onBind(intent: Intent): IBinder {
        return ScoreboardBinding()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID,
            CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
    }

    private fun setSelectedDeviceId(deviceId: String?) {
        selectedDeviceId.value = deviceId
        socketJob?.cancel("Device connection changed", null)

        deviceId?.let { id ->
            socketJob = CoroutineScope(Dispatchers.IO).launch {
                telemetryManager.getFlow(id)?.collect { event ->
                    try {
                        val payload = gson.fromJson<ScoreboardFragment.PhyPayload>(event.payload, phyType)
                        val payloadValue = payload.snr1000
                        socket.sendUdpMessage("${id},${payload.timeStamp},$payloadValue")
                    } catch (e: Exception) {
                        Log.w(ScoreboardFragment.TAG, "Can't parse telemetry event payload", e)
                    }
                }
            }
        }
    }

    inner class ScoreboardBinding : Binder() {
        val deviceIdList = deviceIds.asStateFlow()
        val selectedDeviceId = this@ScoreboardService.selectedDeviceId.asStateFlow()

        fun connectDevice(deviceId: String): Flow<ClientTelemetryEvent>? {
            telemetryManager.connectDevice(deviceId)
            return telemetryManager.getFlow(deviceId)
        }

        fun disconnectDevice(deviceId: String) {
            telemetryManager.disconnectDevice(deviceId)
        }

        fun selectDevice(deviceId: String?) {
            setSelectedDeviceId(deviceId)
        }
    }

    companion object {
        val TAG: String = ScoreboardService::class.java.simpleName

        private const val CHANNEL_ID: String = "foreground_phy"
        private const val CHANNEL_NAME: String = "foreground_phy_name"
        private const val NOTIFICATION_ID = 34569
    }
}