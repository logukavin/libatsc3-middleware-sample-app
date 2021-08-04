package com.nextgenbroadcast.mobile.middleware.dev.telemetry.writer

import android.content.Context
import android.content.Intent
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.TelemetryEvent
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.RfPhyData
import com.vuzix.connectivity.sdk.Connectivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull


class VuzixPhyTelemetryWriter(
    context: Context,
    private val deviceId: String
) : ITelemetryWriter {
    private val connectivity = Connectivity.get(context)

    override fun open() {
    }

    override fun close() {
    }

    override suspend fun write(eventFlow: Flow<TelemetryEvent>) {
        // drop if Vuzix Connectivity framework is not available or you are not linked to a remote device
        if (!connectivity.isAvailable || !connectivity.isLinked) return

        eventFlow.filter { event ->
            event.topic == TelemetryEvent.EVENT_TOPIC_PHY
        }.mapNotNull { event ->
            event.payload as? RfPhyData
        }.collect { phyData ->
            try {
                connectivity.sendBroadcast(Intent(ACTION).apply {
                    setPackage(PACKAGE)
                    putExtra(EXTRA_DEVICE_ID, deviceId)
                    putExtra(EXTRA_TIMESTAMP, phyData.timeStamp)
                    putExtra(EXTRA_SNR, phyData.snr1000)
                })
            } catch (e: Exception) {
                LOG.e(TAG, "Failed to publish with Vuzix connectivity", e)
            }
        }
    }

    companion object {
        val TAG: String = VuzixPhyTelemetryWriter::class.java.simpleName

        private const val PACKAGE = "com.nextgen.vuzixmonitor"
        private const val ACTION = "$PACKAGE.action.TELEMETRY"

        private const val EXTRA_DEVICE_ID = "extra_device_id"
        private const val EXTRA_TIMESTAMP = "extra_timestamp"
        private const val EXTRA_SNR = "extra_snr"
    }
}