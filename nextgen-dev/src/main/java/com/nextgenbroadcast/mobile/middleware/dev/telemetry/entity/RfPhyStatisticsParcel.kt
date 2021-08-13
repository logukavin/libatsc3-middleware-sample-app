package com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.ngbp.libatsc3.middleware.android.phy.models.RfPhyStatistics

@Parcelize
@TypeParceler<RfPhyStatistics, RfPhyStatisticsParceler>()
class RfPhyStatisticsParcel(val payload: RfPhyStatistics): Parcelable

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
