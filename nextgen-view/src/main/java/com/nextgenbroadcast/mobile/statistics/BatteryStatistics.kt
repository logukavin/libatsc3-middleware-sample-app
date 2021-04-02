package com.nextgenbroadcast.mobile.statistics

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@ExperimentalCoroutinesApi
class BatteryStatistics(val context: Context) {

    private val batteryStatus: Intent?
    var isListening = false

    init {
        batteryStatus = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { intentFilter ->
            context.registerReceiver(null, intentFilter)
        }
    }

    fun batteryStatisticsFlow(isStartListening: Boolean): Flow<BatteryStatisticsData?> = flow {
        isListening = isStartListening
        while (!isListening) {
            emit(getBatteryStatus())
            delay(BATTERY_MEASURING_FREQUENCY)
        }
    }

    private fun getBatteryStatus(): BatteryStatisticsData? {

        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val usbCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val acCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

        return BatteryStatisticsData(batteryPct?.toInt()?:0, status, isCharging, chargePlug, usbCharge, acCharge)
    }

    companion object {
        const val BATTERY_MEASURING_FREQUENCY = 250L
    }

}

data class BatteryStatisticsData(
        val batteryPct: Int,
        val status: Int,
        val isCharging: Boolean,
        val chargePlug: Int,
        val usbCharge: Boolean,
        val acCharge: Boolean)