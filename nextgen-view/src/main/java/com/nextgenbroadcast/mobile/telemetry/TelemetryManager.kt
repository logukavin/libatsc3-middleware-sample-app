package com.nextgenbroadcast.mobile.telemetry

import androidx.lifecycle.asLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

class TelemetryManager {
    private val gson = Gson()
    private val telemetryObserver = WebTelemetryObserver(CONNECTION_HOST, CONNECTION_PORT, TOPICS)
    private val eventFlow = MutableSharedFlow<TelemetryEvent>(
            replay = 30,
            extraBufferCapacity = 0,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var observingJob: Job? = null

    val phyEvents = eventFlow.filter { event ->
        event.topic == EVENT_TOPIC_PHY
    }.map { event ->
        gson.fromJson<PhyPayload>(event.payload, typeToken<PhyPayload>())
    }.asLiveData()

    val pingEvents = eventFlow.filter { event ->
        event.topic == EVENT_TOPIC_PING
    }.map { event ->
        gson.fromJson<PongPayload>(event.payload, typeToken<PongPayload>())
    }.asLiveData()

    fun start() {
        if (observingJob != null) return

        observingJob = CoroutineScope(Dispatchers.IO).launch {
            delay(3000)

            if (isActive) {
                telemetryObserver.read(eventFlow)
            }
        }
    }

    fun stop() {
        observingJob?.cancel()
        observingJob = null
    }

    inline fun <reified T> typeToken() = object: TypeToken<T>() {}.type

    data class PhyPayload(
            val snr1000: Int
    )

    data class PongPayload(
            val name: String,
            val timeStamp: Long
    )

    companion object {
        private const val CONNECTION_HOST = "localhost"
        private const val CONNECTION_PORT = 8081

        const val EVENT_TOPIC_PHY = "phy"
        const val EVENT_TOPIC_PING = "ping"

        private val TOPICS = listOf(
                EVENT_TOPIC_PING,
                EVENT_TOPIC_PHY
        )
    }
}