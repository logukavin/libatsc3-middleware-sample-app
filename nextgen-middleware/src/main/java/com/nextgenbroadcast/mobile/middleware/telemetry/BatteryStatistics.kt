package com.nextgenbroadcast.mobile.middleware.telemetry

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIoTEvent
import com.nextgenbroadcast.mobile.middleware.telemetry.aws.AWSIotThing
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.TimeUnit

class BatteryStatistics(
        context: Context,
        private val updateInterval: Long = BATTERY_MEASURING_FREQUENCY
) {
    private val appContext = context.applicationContext
    private val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

    suspend fun start(eventFlow: MutableSharedFlow<AWSIoTEvent>) {
        supervisorScope {
            while (isActive) {
                appContext.registerReceiver(null, intentFilter)?.let { batteryStatus ->
                    val level: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val batteryPct = level * 100 / scale.toFloat()

                    eventFlow.emit(AWSIoTEvent(
                            AWSIotThing.AWSIOT_TOPIC_BATTERY,
                            batteryPct
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