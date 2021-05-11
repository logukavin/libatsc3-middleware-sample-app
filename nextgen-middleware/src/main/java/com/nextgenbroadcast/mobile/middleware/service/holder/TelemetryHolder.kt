package com.nextgenbroadcast.mobile.middleware.service.holder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import androidx.media.MediaBrowserServiceCompat
import com.google.firebase.installations.FirebaseInstallations
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.middleware.Atsc3ReceiverCore
import com.nextgenbroadcast.mobile.middleware.BuildConfig
import com.nextgenbroadcast.mobile.middleware.ServiceDialogActivity
import com.nextgenbroadcast.mobile.middleware.encryptedSharedPreferences
import com.nextgenbroadcast.mobile.middleware.gateway.web.ConnectionType
import com.nextgenbroadcast.mobile.middleware.repository.TelemetrySharedPreferences
import com.nextgenbroadcast.mobile.middleware.repository.TelemetrySharedPreferences.Companion.DEBUG_KEY
import com.nextgenbroadcast.mobile.middleware.repository.TelemetrySharedPreferences.Companion.PHY_KEY
import com.nextgenbroadcast.mobile.middleware.server.web.IMiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService
import com.nextgenbroadcast.mobile.middleware.telemetry.ReceiverTelemetry
import com.nextgenbroadcast.mobile.middleware.telemetry.ReceiverTelemetry.INFO_DEBUG
import com.nextgenbroadcast.mobile.middleware.telemetry.ReceiverTelemetry.INFO_PHY
import com.nextgenbroadcast.mobile.middleware.telemetry.RemoteControlBroker
import com.nextgenbroadcast.mobile.middleware.telemetry.TelemetryBroker
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIoThing
import com.nextgenbroadcast.mobile.middleware.telemetry.control.AWSIoTelemetryControl
import com.nextgenbroadcast.mobile.middleware.telemetry.control.ITelemetryControl
import com.nextgenbroadcast.mobile.middleware.telemetry.control.UdpTelemetryControl
import com.nextgenbroadcast.mobile.middleware.telemetry.control.WebTelemetryControl
import com.nextgenbroadcast.mobile.middleware.telemetry.reader.BatteryTelemetryReader
import com.nextgenbroadcast.mobile.middleware.telemetry.reader.GPSTelemetryReader
import com.nextgenbroadcast.mobile.middleware.telemetry.reader.RfPhyTelemetryReader
import com.nextgenbroadcast.mobile.middleware.telemetry.reader.SensorTelemetryReader
import com.nextgenbroadcast.mobile.middleware.telemetry.task.PongTelemetryTask
import com.nextgenbroadcast.mobile.middleware.telemetry.task.WiFiInfoTelemetryTask
import com.nextgenbroadcast.mobile.middleware.telemetry.writer.AWSIoTelemetryWriter
import com.nextgenbroadcast.mobile.middleware.telemetry.writer.FileTelemetryWriter
import com.nextgenbroadcast.mobile.middleware.telemetry.writer.WebTelemetryWriter
import kotlinx.coroutines.flow.*
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess

internal class TelemetryHolder(
        private val context: Context,
        private val receiver: Atsc3ReceiverCore
) {
    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private val wifiManager: WifiManager by lazy {
        context.getSystemService(MediaBrowserServiceCompat.WIFI_SERVICE) as WifiManager
    }

    private var telemetryBroker: TelemetryBroker? = null
    private var remoteControl: RemoteControlBroker? = null
    private var awsIoThing: AWSIoThing? = null

    private val _debugInfoSettings = MutableStateFlow(mapOf(
            ReceiverTelemetry.INFO_DEBUG to true
    ))

    val debugInfoSettings = _debugInfoSettings.asStateFlow()
    val telemetryEnabled: StateFlow<Map<String, Boolean>>
        get() = telemetryBroker?.readersEnabled ?: MutableStateFlow(emptyMap())
    val telemetryDelay: StateFlow<Map<String, Long>>
        get() = telemetryBroker?.readersDelay ?: MutableStateFlow(emptyMap())

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(TelemetrySharedPreferences.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    fun open() {
        telemetryBroker = TelemetryBroker(
                listOf(
                        BatteryTelemetryReader(context),
                        SensorTelemetryReader(sensorManager, Sensor.TYPE_LINEAR_ACCELERATION),
                        SensorTelemetryReader(sensorManager, Sensor.TYPE_GYROSCOPE),
                        SensorTelemetryReader(sensorManager, Sensor.TYPE_SIGNIFICANT_MOTION),
                        SensorTelemetryReader(sensorManager, Sensor.TYPE_STEP_DETECTOR),
                        SensorTelemetryReader(sensorManager, Sensor.TYPE_STEP_COUNTER),
                        SensorTelemetryReader(sensorManager, Sensor.TYPE_ROTATION_VECTOR),
                        GPSTelemetryReader(context),
                        RfPhyTelemetryReader(receiver.rfPhyMetricsFlow)
                ),
                listOf(
                        //AWSIoTelemetryWriter(thing),
                        //FileTelemetryWriter(cacheDir, "telemetry.log")
                )
        ).apply {
            //start() do not start Telemetry with application, use switch in Settings dialog or AWS command
        }

        remoteControl = RemoteControlBroker(
                listOf(
                        //AWSIoTelemetryControl(thing),
                        //WebTelemetryControl(),
                        UdpTelemetryControl(CONNECTION_HOST, CONNECTION_UDP_PORT)
                ),
                ::executeCommand
        ).apply {
            start()
        }

        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                initializeAWSIoThing(task.result)
            } else {
                LOG.e(TAG, "Can't create Telemetry because Firebase ID not received.", task.exception)
            }
        }

        _debugInfoSettings.value = mutableMapOf<String, Boolean>().apply {
            put(INFO_DEBUG, sharedPreferences.getBoolean(DEBUG_KEY, false))
            put(INFO_PHY, sharedPreferences.getBoolean(PHY_KEY, false))
        }

    }

    fun close() {
        telemetryBroker?.close()
        telemetryBroker = null

        remoteControl?.stop()
        remoteControl = null

        awsIoThing?.close()
        awsIoThing = null
    }

    fun setAllEnabled(enabled: Boolean) {
        telemetryBroker?.setReadersEnabled(enabled)
    }

    fun setTelemetryEnabled(enabled: Boolean, name: String) {
        telemetryBroker?.setReaderEnabled(enabled, name)
    }

    fun setTelemetryDelay(delayMils: Long, name: String) {
        telemetryBroker?.setReaderDelay(name, delayMils)
    }

    fun notifyWebServerStarted(server: IMiddlewareWebServer) {
        server.addConnection(CONNECTION_TYPE, CONNECTION_HOST, CONNECTION_TCP_PORT)

        remoteControl?.addControl(WebTelemetryControl(server))
        telemetryBroker?.addWriter(WebTelemetryWriter(server))
    }

    fun notifyWebServerStopped() {
        remoteControl?.removeControl(WebTelemetryControl::class.java)
        telemetryBroker?.removeWriter(WebTelemetryWriter::class.java)
    }

    private fun initializeAWSIoThing(serialNumber: String) {
        val thing = AWSIoThing(serialNumber,
                encryptedSharedPreferences(context, IoT_PREFERENCE),
                context.assets
        ).also {
            awsIoThing = it
        }

        telemetryBroker?.addWriter(
                AWSIoTelemetryWriter(thing)
        )

        remoteControl?.addControl(
                AWSIoTelemetryControl(thing)
        )
    }

    private fun executeCommand(action: String, arguments: Map<String, String>) {
        LOG.d(TelemetryBroker.TAG, "Control command received: $action, args: $arguments")

        when (action) {
            ITelemetryControl.CONTROL_ACTION_TUNE -> {
                arguments[ITelemetryControl.CONTROL_ARGUMENT_FREQUENCY]?.let { frequencyList ->
                    val frequencies = frequencyList
                            .split(ITelemetryControl.CONTROL_ARGUMENT_DELIMITER)
                            .mapNotNull { it.toIntOrNull() }
                    if (frequencies.isNotEmpty()) {
                        receiver.tune(PhyFrequency.user(frequencies))
                    }
                }
            }

            ITelemetryControl.CONTROL_ACTION_ACQUIRE_SERVICE -> {
                val service = arguments[ITelemetryControl.CONTROL_ARGUMENT_SERVICE_ID]?.toIntOrNull()?.let { serviceId ->
                    arguments[ITelemetryControl.CONTROL_ARGUMENT_SERVICE_BSID]?.toIntOrNull()?.let { bsid ->
                        receiver.findServiceById(bsid, serviceId)
                    } ?: let {
                        receiver.findActiveServiceById(serviceId)
                    }
                }
                        ?: arguments[ITelemetryControl.CONTROL_ARGUMENT_SERVICE_NAME]?.let { serviceName ->
                            receiver.findServiceByName(serviceName)
                        }

                if (service != null) {
                    receiver.selectService(service)
                }
            }

            ITelemetryControl.CONTROL_ACTION_SET_TEST_CASE -> {
                telemetryBroker?.testCase = arguments[ITelemetryControl.CONTROL_ARGUMENT_CASE]?.ifBlank {
                    null
                }
            }

            ITelemetryControl.CONTROL_ACTION_RESTART_APP -> {
                val delay = max(arguments[ITelemetryControl.CONTROL_ARGUMENT_START_DELAY]?.toLongOrNull()
                        ?: 0, 100L)
                val intent = Intent(ServiceDialogActivity.ACTION_WATCH_TV).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                alarmManager[AlarmManager.RTC, System.currentTimeMillis() + delay] = PendingIntent.getActivity(
                        context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

                exitProcess(0)
            }

            ITelemetryControl.CONTROL_ACTION_REBOOT_DEVICE -> {
                try {
                    // maybe arrayOf("/system/bin/su", "-c", "reboot now")
                    Runtime.getRuntime().exec("shell execute reboot")
                } catch (e: Exception) {
                    LOG.d(Atsc3ForegroundService.TAG, "Can't reboot device", e)
                }
            }

            ITelemetryControl.CONTROL_ACTION_TELEMETRY_ENABLE -> {
                val telemetryNameList = arguments[ITelemetryControl.CONTROL_ARGUMENT_NAME]?.let { telemetryNameList ->
                    telemetryNameList.split(ITelemetryControl.CONTROL_ARGUMENT_DELIMITER).map { name ->
                        SensorTelemetryReader.getFullSensorName(name) ?: name
                    }
                }

                arguments[ITelemetryControl.CONTROL_ARGUMENT_ENABLE]?.let {
                    val enabled = it.toBoolean()
                    if (telemetryNameList != null) {
                        telemetryBroker?.setReaderEnabled(enabled, telemetryNameList)
                    } else {
                        telemetryBroker?.setReadersEnabled(enabled)
                    }
                }
            }

            ITelemetryControl.CONTROL_ACTION_SHOW_DEBUG_INFO -> {
                _debugInfoSettings.value = mutableMapOf<String, Boolean>().apply {
                    arguments.forEach { (key, value) ->
                        put(key, value.toBoolean())
                        sharedPreferences.edit().putBoolean(key, value.toBoolean()).apply()
                    }
                }
            }

            ITelemetryControl.CONTROL_ACTION_VOLUME -> {
                arguments[ITelemetryControl.CONTROL_ARGUMENT_VALUE]?.toIntOrNull()?.let { inputVolume ->
                    val volume = min(max(0, inputVolume), 100) / 100f
                    val maxStreamVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxStreamVolume * volume).toInt(), 0)
                }
            }

            ITelemetryControl.CONTROL_ACTION_WIFI_INFO -> {
                telemetryBroker?.runTask(WiFiInfoTelemetryTask(wifiManager))
            }

            ITelemetryControl.CONTROL_ACTION_PING -> {
                telemetryBroker?.runTask(PongTelemetryTask())
            }

            ITelemetryControl.CONTROL_ACTION_FILE_WRITER -> {
                val fileName = arguments[ITelemetryControl.CONTROL_ARGUMENT_NAME]
                val writeDuration = arguments[ITelemetryControl.CONTROL_ARGUMENT_DURATION]?.toIntOrNull()
                telemetryBroker?.removeWriter(FileTelemetryWriter::class.java)

                if (!fileName.isNullOrBlank()) {
                    telemetryBroker?.addWriter(FileTelemetryWriter(context.filesDir, fileName, writeDuration ?: 0), false)
                }
            }

            ITelemetryControl.CONTROL_ACTION_RESET_RECEIVER_DEMODE -> {
                /// TODO should be implemented
            }
        }

    }

    companion object {
        val TAG: String = TelemetryHolder::class.java.simpleName

        private const val IoT_PREFERENCE = "${BuildConfig.LIBRARY_PACKAGE_NAME}.awsiot"

        private val CONNECTION_TYPE = ConnectionType.HTTP
        private const val CONNECTION_HOST = "0.0.0.0"
        private const val CONNECTION_TCP_PORT = 8081
        private const val CONNECTION_UDP_PORT = 6969
    }
}