package com.nextgenbroadcast.mobile.middleware.dev.telemetry.writer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.TelemetryEvent
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.BatteryData
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.RfPhyData
import com.vuzix.connectivity.sdk.Connectivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.ngbp.libatsc3.middleware.android.phy.models.RfPhyStatistics


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

        eventFlow.mapNotNull { event ->
            when (event.topic) {
                TelemetryEvent.EVENT_TOPIC_PHY -> {
                    (event.payload as? RfPhyData)?.let {
                        RfPhyStatisticsParcel(it.stat)
                    }
                }

                TelemetryEvent.EVENT_TOPIC_BATTERY -> {
                    (event.payload as? BatteryData)?.let {
                        BatteryDataParcel(it)
                    }
                }

                else -> null
            }?.let { payload ->
                Bundle().apply {
                    putString(EXTRA_DEVICE_ID, deviceId)
                    putString(EXTRA_TYPE, event.topic)
                    putParcelable(EXTRA_PAYLOAD, payload)
                }
            }
        }.collect { extras ->
            try {
                connectivity.sendBroadcast(Intent(ACTION).apply {
                    setPackage(PACKAGE)
                    replaceExtras(extras)
                })
            } catch (e: Exception) {
                LOG.e(TAG, "Failed to publish with Vuzix connectivity", e)
            }
        }
    }

    object BatteryDataParceler : Parceler<BatteryData> {
        override fun create(parcel: Parcel) = BatteryData(parcel.readFloat())

        override fun BatteryData.write(parcel: Parcel, flags: Int) {
            parcel.writeFloat(level)
        }
    }

    @Parcelize
    @TypeParceler<BatteryData, BatteryDataParceler>()
    class BatteryDataParcel(val payload: BatteryData): Parcelable

    object RfPhyStatisticsParceler : Parceler<RfPhyStatistics> {
        override fun create(parcel: Parcel) = RfPhyStatistics(
            parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(),
            parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(),
            parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt()
        )

        override fun RfPhyStatistics.write(parcel: Parcel, flags: Int) {
            parcel.writeInt(tuner_lock)
            parcel.writeInt(rssi)
            parcel.writeInt(modcod_valid_0)
            parcel.writeInt(plp_fec_type_0)
            parcel.writeInt(plp_mod_0)
            parcel.writeInt(plp_cod_0)
            parcel.writeInt(rfLevel1000)
            parcel.writeInt(snr1000_global)
            parcel.writeInt(ber_pre_ldpc_0)
            parcel.writeInt(ber_pre_bch_0)
            parcel.writeInt(fer_post_bch_0)
            parcel.writeInt(demod_lock)
            parcel.writeInt(cpu_status)
            parcel.writeInt(plp_lock_any)
            parcel.writeInt(plp_lock_all)
        }
    }

    @Parcelize
    @TypeParceler<RfPhyStatistics, RfPhyStatisticsParceler>()
    class RfPhyStatisticsParcel(val payload: RfPhyStatistics): Parcelable

    companion object {
        val TAG: String = VuzixPhyTelemetryWriter::class.java.simpleName

        private const val PACKAGE = "com.nextgen.vuzixmonitor"
        private const val ACTION = "$PACKAGE.action.TELEMETRY"

        private const val EXTRA_DEVICE_ID = "extra_device_id"
        private const val EXTRA_TYPE = "extra_type"
        private const val EXTRA_PAYLOAD = "extra_payload"
    }
}