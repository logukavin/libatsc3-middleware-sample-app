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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class ScoreboardService : Service() {
    private val gson = Gson()
    private val phyType = object : TypeToken<ScoreboardFragment.PhyPayload>() {}.type
    private val deviceIds = MutableStateFlow<List<TelemetryDevice>>(emptyList())
    private val selectedDeviceId = MutableStateFlow<String?>(null)
    private val commandBackLogFlow = MutableSharedFlow<String>(1, 5, BufferOverflow.DROP_OLDEST)
    private val dateFormat = SimpleDateFormat(COMMAND_DATE_FORMAT, Locale.US)

    private val socket: DatagramSocketWrapper by lazy {
        DatagramSocketWrapper(applicationContext)
    }
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

        val scoreboardIntent = Intent(this, ScoreboardPagerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }.let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        val closeIntent = Intent(this, ScoreboardService::class.java).apply {
            action = ACTION_STOP
        }.let { intent ->
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        val consoleIntent = Intent(this, ConsoleActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }.let { intent ->
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        createNotificationChannel()

        customNotificationView = RemoteViews(packageName, R.layout.scoreboard_notification_view).apply {
            setOnClickPendingIntent(R.id.notification_stop_service_button, closeIntent)
            setOnClickPendingIntent(R.id.notification_open_console_btn, consoleIntent)
            setTextViewText(R.id.notification_text, getNotificationDescription())
        }

        notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.app_name))
            .setSmallIcon(R.drawable.notifiaction_icon)
            .setColor(getColor(R.color.green))
            .setContentIntent(scoreboardIntent)
            .setCustomContentView(customNotificationView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun getNotificationDescription(): String {
        val broadcastText = resources.getString(R.string.notification_broadcasting)
        val deviceId = selectedDeviceId.value ?: resources.getString(R.string.notification_none)
        return "$broadcastText $deviceId"
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_STOP -> stopSelf()
            ACTION_COMMANDS -> sendCommand(intent)
            ACTION_GLOBAL_COMMANDS -> sendGlobalCommand(intent)
        }

        return START_NOT_STICKY
    }

    private fun sendCommand(intent: Intent) {
        val deviceList = intent.getStringArrayListExtra(DEVICES_EXTRAS)
        val commandStr = intent.getStringExtra(COMMAND_EXTRAS)

        if (!deviceList.isNullOrEmpty()) {
            commandStr?.let {
                telemetryManager.sendDeviceCommand(deviceList, commandStr)

                commandBackLogFlow.tryEmit(commandStr.commandFormat("<<"))
            }
        }
    }

    private fun sendGlobalCommand(intent: Intent) {
        intent.getStringExtra(CommandFragment.TOPIC)?.let { topic ->
            intent.getStringExtra(COMMAND_EXTRAS)?.let { payload ->
                telemetryManager.sendGlobalCommand(topic, payload)
            }
        }
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

    private fun String.commandFormat(prefix: String): String {
        return "$prefix ${dateFormat.format(Date())} : $this"
    }

    inner class ScoreboardBinding : Binder() {
        val deviceIdList = deviceIds.asStateFlow()
        val selectedDeviceId = this@ScoreboardService.selectedDeviceId.asStateFlow()

        @Volatile
        private var backLogFlow: SharedFlow<String>? = null

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

        private fun getGlobalEventLogFlow(): Flow<String>? {
            return telemetryManager.getGlobalEventFlow()?.map { event ->
                event.toString().commandFormat(">>")
            }
        }

        @Synchronized
        fun getBacklogFlow(): Flow<String>? {
            val flow = backLogFlow
            return flow ?: let {
                getGlobalEventLogFlow()?.let { globalFlow ->
                    flowOf(globalFlow, commandBackLogFlow)
                        .flattenMerge()
                        .shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.Eagerly, 50)
                }.also {
                    backLogFlow = it
                }
            }
        }
    }

    companion object {
        val TAG: String = ScoreboardService::class.java.simpleName

        private const val CHANNEL_ID: String = "foreground_phy"
        private const val CHANNEL_NAME: String = "foreground_phy_name"
        private const val NOTIFICATION_ID = 34569
        private const val ACTION_STOP = "${BuildConfig.APPLICATION_ID}.action_stop"

        const val ACTION_COMMANDS = "${BuildConfig.APPLICATION_ID}.commands"
        const val ACTION_GLOBAL_COMMANDS = "${BuildConfig.APPLICATION_ID}.global_commands"
        const val COMMAND_EXTRAS = "commands"
        const val DEVICES_EXTRAS = "devices_id_list"

        const val COMMAND_DATE_FORMAT = "HH:mm:ss.SSS"
    }
}