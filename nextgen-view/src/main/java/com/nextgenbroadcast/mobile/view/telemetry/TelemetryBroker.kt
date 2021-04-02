package com.nextgenbroadcast.mobile.view.telemetry

import android.content.Context
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.core.telemetry.aws.AWSIoTEvent
import com.nextgenbroadcast.mobile.core.telemetry.aws.AWSIotThing
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.Exception

class TelemetryBroker(
        context: Context
) {
    private val appContext = context.applicationContext
    private val thing = AWSIotThing(appContext)
    private val eventFlow = MutableSharedFlow<AWSIoTEvent>(replay = 20, extraBufferCapacity = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var job: Job? = null

    fun start() {
        thing.connect()

        job = GlobalScope.launch {
            try {
                launch {
                    BatteryStatistics(appContext).start(eventFlow)
                }

                eventFlow.collect { event ->
                    thing.publish(event.topic, event.payload)
                }
            } catch (e: Exception) {
                LOG.d(TAG, "Telemetry gethering error: ", e)
            }
        }
    }

    fun stop() {
        thing.disconnect()
        job?.cancel()
    }

    companion object {
        val TAG: String = TelemetryBroker::class.java.simpleName
    }
}