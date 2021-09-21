package com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.ClientTelemetryEvent
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.TelemetryEvent
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.BatteryData
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.RfPhyData
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TDataPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.selects.select

private val gson = Gson()
private val phyType = object : TypeToken<RfPhyData>() {}.type
private val batteryType = object : TypeToken<BatteryData>() {}.type

fun Flow<ClientTelemetryEvent>.mapToEvent(): Flow<TelemetryEvent> = mapNotNull { event ->
    try {
        when (event.topic) {
            TelemetryEvent.EVENT_TOPIC_PHY -> gson.fromJson<RfPhyData>(event.payload, phyType)
            TelemetryEvent.EVENT_TOPIC_BATTERY ->  gson.fromJson<BatteryData>(event.payload, batteryType)
            else -> null
        }?.let { payload ->
            TelemetryEvent(event.topic, payload)
        }
    } catch (e: Exception) {
        LOG.w("Flow.mapToDataPoint", "Can't parse telemetry event payload", e)
        null
    }
}

fun Flow<TelemetryEvent>.mapToDataPoint(): Flow<TDataPoint> = mapNotNull { event ->
    try {
        if (event.topic == TelemetryEvent.EVENT_TOPIC_PHY) {
            val payload = event.payload as RfPhyData
            val payloadValue = payload.stat.snr1000_global.toDouble()
            val timestamp = payload.timeStamp
            TDataPoint(timestamp, payloadValue)
        } else null
    } catch (e: Exception) {
        LOG.w("Flow.mapToDataPoint", "Can't parse telemetry event payload", e)
        null
    }
}

private val DONE = Any()
private val NULL = Any()

fun Flow<TDataPoint>.sampleTelemetry(scope: CoroutineScope, tickDelayMillis: Long, emptyDelayMillis: Long): Flow<TDataPoint> {
    val values = produceIn(scope)
    return flow {
        var lastTime = 0L
        var lastDataSentAt = 0L

        var lastValue: Any? = null
        val ticker = ticker(tickDelayMillis)
        while (lastValue !== DONE) {
            select<Unit> {
                values.onReceiveCatching { result ->
                    result
                        .onSuccess {
                            lastValue = it
                            lastTime = it.timestamp
                            lastDataSentAt = System.currentTimeMillis()
                            emit(it)
                        }
                        .onFailure {
                            it?.let { throw it }
                            ticker.cancel(/*ChildCancelledException()*/)
                            lastValue = DONE
                        }
                }

                ticker.onReceive {
                    val timeDiff = System.currentTimeMillis() - lastDataSentAt
                    if (lastValue == null || timeDiff > emptyDelayMillis && lastTime > 0) {
                        emit(TDataPoint(lastTime + timeDiff, 0.0))
                    }
                }
            }
        }
    }
}
