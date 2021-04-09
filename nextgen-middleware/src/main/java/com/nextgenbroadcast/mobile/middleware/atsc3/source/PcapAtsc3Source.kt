package com.nextgenbroadcast.mobile.middleware.atsc3.source

import org.ngbp.libatsc3.middleware.android.phy.Atsc3NdkPHYClientBase
import org.ngbp.libatsc3.middleware.android.phy.virtual.PcapDemuxedVirtualPHYAndroid
import org.ngbp.libatsc3.middleware.android.phy.virtual.PcapSTLTPVirtualPHYAndroid

abstract class PcapAtsc3Source(
        private val type: PcapType
) : BaseAtsc3Source() {

    enum class PcapType {
        DEMUXED, STLTP
    }

    protected fun createPhyClient(): Atsc3NdkPHYClientBase {
        return when (type) {
            PcapType.DEMUXED -> PcapDemuxedVirtualPHYAndroid()
            PcapType.STLTP -> PcapSTLTPVirtualPHYAndroid()
        }.apply {
            init()
        }
    }
}