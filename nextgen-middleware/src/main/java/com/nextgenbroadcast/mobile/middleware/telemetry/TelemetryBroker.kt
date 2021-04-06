package com.nextgenbroadcast.mobile.middleware.telemetry

import android.content.Context
import android.content.SharedPreferences
import android.hardware.SensorManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.BuildConfig
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIoTControl
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIoTEvent
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIotThing
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.Exception

class TelemetryBroker(
        context: Context,
        private val onCommand: suspend (action: String, arguments: Map<String, String>) -> Unit
) {
    private val appContext = context.applicationContext
    private val preferences: SharedPreferences = EncryptedSharedPreferences.create(
            appContext,
            IoT_PREFERENCE,
            MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val thing = AWSIotThing(preferences, appContext.assets)
    private val eventFlow = MutableSharedFlow<AWSIoTEvent>(replay = 20, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    // it's MUST be non suspending because we use tryEmit in callback
    private val commandFlow = MutableSharedFlow<AWSIoTControl>(replay = 1, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var job: Job? = null

    fun start() {
        thing.connect()

        job = GlobalScope.launch {
            try {
                launch {
                    BatteryTelemetry(appContext).start(eventFlow)
                }

                launch {
                    LinearAccelerationTelemetry(sensorManager).start(eventFlow)
                }

                launch {
                    GyroscopeSensorTelemetry(sensorManager).start(eventFlow)
                }

                launch {
                    SignificantMotionSensorTelemetry(sensorManager).start(eventFlow)
                }

                launch {
                    StepDetectorSensorTelemetry(sensorManager).start(eventFlow)
                }

                launch {
                    StepCounterSensorTelemetry(sensorManager).start(eventFlow)
                }

                launch {
                    thing.subscribeCommandsFlow(commandFlow)
                }
                launch {
                    commandFlow.collect { command ->
                        onCommand(command.action, command.arguments)
                    }
                }
                launch {
                    RotationVectorSensorTelemetry(sensorManager).start(eventFlow)
                }

                eventFlow.collect { event ->
                    thing.publish(event.topic, event.payload)
                    LOG.d(TAG, "AWS IoT event: ${event.topic} - ${event.payload}")
                }
            } catch (e: Exception) {
                LOG.d(TAG, "Telemetry gathering error: ", e)
            }
        }
    }

    fun stop() {
        thing.disconnect()
        job?.cancel()
    }

    companion object {
        val TAG: String = TelemetryBroker::class.java.simpleName

        private const val IoT_PREFERENCE = "${BuildConfig.LIBRARY_PACKAGE_NAME}.awsiot"
    }
}
