package com.nextgenbroadcast.mobile.middleware.telemetry.reader

import com.nextgenbroadcast.mobile.middleware.telemetry.ReceiverTelemetry
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryEvent
import com.nextgenbroadcast.mobile.middleware.telemetry.entity.TelemetryPayload
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import org.ngbp.libatsc3.middleware.android.phy.models.RfPhyFecModCodTypes
import org.ngbp.libatsc3.middleware.android.phy.models.RfPhyStatistics

class RfPhyTelemetryReader(
        private val flow: SharedFlow<RfPhyStatistics>
) : ITelemetryReader {

    override val name = NAME
    override var delayMils: Long = -1

    //jjustman-2021-06-07 - TODO: add additional SL phy
    override suspend fun read(eventFlow: MutableSharedFlow<TelemetryEvent>) {
        flow.collect { rfStatistic ->
            eventFlow.emit(TelemetryEvent(TelemetryEvent.EVENT_TOPIC_PHY, RfPhyData(
                    tuner_lock = rfStatistic.tuner_lock == 1,
                    demod_lock = rfStatistic.demod_lock == 1,
                    plp_lock_any = rfStatistic.demod_lock == 1,
                    plp_lock_all = rfStatistic.demod_lock == 1,
                    cpu_status = if (rfStatistic.cpu_status == 1) 'R' else 'H',
                    rssi1000 = (1000.0 * (String.format("%d.%03d", rfStatistic.rssi / 1000, -rfStatistic.rssi % 1000).toDoubleOrNull() ?: 0.0)).toLong(),
                    snr1000 = rfStatistic.snr1000_global,
                    modcod_valid_0 = rfStatistic.modcod_valid_0 == 1,
                    plp_fec_type_0 = RfPhyFecModCodTypes.L1d_PlpFecType.getOrDefault(rfStatistic.plp_fec_type_0, RfPhyFecModCodTypes.L1d_PlpFecType[255]),
                    plp_mod_0 = RfPhyFecModCodTypes.L1d_PlpMod.getOrDefault(rfStatistic.plp_mod_0, RfPhyFecModCodTypes.L1d_PlpMod[255]),
                    plp_cod_0 = RfPhyFecModCodTypes.L1d_PlpCod.getOrDefault(rfStatistic.plp_cod_0, RfPhyFecModCodTypes.L1d_PlpCod[255]),
                    ber_pre_ldpc_0 = rfStatistic.ber_pre_ldpc_0,
                    ber_pre_bch_0 = rfStatistic.ber_pre_bch_0,
                    fer_post_bch_0 = rfStatistic.fer_post_bch_0
            )))
        }
    }

    companion object {
        const val NAME = ReceiverTelemetry.TELEMETRY_PHY
    }
}

private data class RfPhyData(
        val tuner_lock: Boolean,
        val demod_lock: Boolean,
        val plp_lock_any: Boolean,
        val plp_lock_all: Boolean,
        val cpu_status: Char,
        val rssi1000: Long,
        val snr1000: Int,
        val modcod_valid_0: Boolean,
        val plp_fec_type_0: String,
        val plp_mod_0: String,
        val plp_cod_0: String,
        val ber_pre_ldpc_0: Int,
        val ber_pre_bch_0: Int,
        val fer_post_bch_0: Int
) : TelemetryPayload()