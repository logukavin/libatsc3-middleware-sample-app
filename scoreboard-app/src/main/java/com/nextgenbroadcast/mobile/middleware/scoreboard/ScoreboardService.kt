package com.nextgenbroadcast.mobile.middleware.scoreboard

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
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
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val broadcastText by lazy {
        resources.getString(R.string.notification_broadcasting)
    }
    private val noneText by lazy {
        resources.getString(R.string.notification_none)
    }

    private lateinit var telemetryManager: TelemetryManager
    private lateinit var notification: Notification
    private var customNotificationView: RemoteViews? = null
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

        val deleteIntent = Intent(this, ScoreboardService::class.java).let { deleteIntent ->
            deleteIntent.action = ACTION_STOP
            PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        createNotificationChannel()

        customNotificationView = RemoteViews(packageName, R.layout.scoreboard_notification_view).apply {
                setOnClickPendingIntent(R.id.notification_stop_service_button, deleteIntent)
                setTextViewText(R.id.notification_text, getNotificationDescription())
            }

        notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.app_name))
            .setSmallIcon(R.drawable.notifiaction_icon)
            .setColor(getColor(R.color.green))
            .setContentIntent(pendingIntent)
            .setCustomContentView(customNotificationView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun getNotificationDescription() =
        "$broadcastText ${selectedDeviceId.value ?: noneText}"

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action == ACTION_STOP) {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return ScoreboardBinding()
    }

    override fun onDestroy() {
        super.onDestroy()

        socketJob?.cancel("Stop service")

        if (this::telemetryManager.isInitialized) {
            telemetryManager.stop()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID,
            CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        notificationManager.createNotificationChannel(channel)
    }

    private fun setSelectedDeviceId(deviceId: String?) {
        customNotificationView?.setTextViewText(R.id.notification_text, getNotificationDescription())
        notificationManager.notify(NOTIFICATION_ID, notification)

        selectedDeviceId.value = deviceId
        socketJob?.cancel("Device connection changed", null)

        deviceId?.let {
            telemetryManager.getDeviceById(deviceId)?.let { device ->
                socketJob = CoroutineScope(Dispatchers.IO).launch {
                    telemetryManager.getFlow(device)?.collect { event ->
                        try {
                            val payload = gson.fromJson<ScoreboardFragment.PhyPayload>(event.payload, phyType)
                            val payloadValue = payload.snr1000
                            socket.sendUdpMessage("${deviceId},${payload.timeStamp},$payloadValue")
                        } catch (e: Exception) {
                            Log.w(ScoreboardFragment.TAG, "Can't parse telemetry event payload", e)
                        }
                    }
                }
            }
        }
    }

    inner class ScoreboardBinding : Binder() {
        val deviceIdList = deviceIds.asStateFlow()
        val selectedDeviceId = this@ScoreboardService.selectedDeviceId.asStateFlow()

        fun connectDevice(deviceId: String): Flow<ClientTelemetryEvent>? {
            return telemetryManager.getDeviceById(deviceId)?.let { device ->
                telemetryManager.getFlow(device) ?: let {
                    telemetryManager.connectDevice(device)
                    telemetryManager.getFlow(device)
                }
            }
        }

        fun disconnectDevice(deviceId: String) {
            telemetryManager.getDeviceById(deviceId)?.let { device ->
                telemetryManager.disconnectDevice(device)
            }
        }

        fun selectDevice(deviceId: String?) {
            setSelectedDeviceId(deviceId)
        }

        fun getConnectedDevices(): List<String> {
            return telemetryManager.getConnectedDevices().map { it.id }
        }
    }

    companion object {
        val TAG: String = ScoreboardService::class.java.simpleName

        private const val CHANNEL_ID: String = "foreground_phy"
        private const val CHANNEL_NAME: String = "foreground_phy_name"
        private const val NOTIFICATION_ID = 34569
        private const val ACTION_STOP = "${BuildConfig.APPLICATION_ID}.action_stop"
    }
}