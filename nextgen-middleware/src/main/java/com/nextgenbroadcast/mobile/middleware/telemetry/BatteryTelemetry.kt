package com.nextgenbroadcast.mobile.middleware.telemetry

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryEvent
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIotThing
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryPayload
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.TimeUnit

class BatteryTelemetry(
        context: Context,
        private val updateInterval: Long = BATTERY_MEASURING_FREQUENCY
) {
    private val appContext = context.applicationContext
    private val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

    suspend fun start(eventFlow: MutableSharedFlow<TelemetryEvent>) {
        supervisorScope {
            while (isActive) {
                appContext.registerReceiver(null, intentFilter)?.let { batteryStatus ->
                    val level: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val batteryPct = level * 100 / scale.toFloat()

                    eventFlow.emit(TelemetryEvent(
                            AWSIotThing.AWSIOT_TOPIC_BATTERY,
                            BatteryData(batteryPct)
                    ))
                }

                delay(updateInterval)
            }
        }
    }

    companion object {
        val BATTERY_MEASURING_FREQUENCY = TimeUnit.MINUTES.toMillis(1)
    }
}

data class BatteryData(
        val level: Float
) : TelemetryPayload()