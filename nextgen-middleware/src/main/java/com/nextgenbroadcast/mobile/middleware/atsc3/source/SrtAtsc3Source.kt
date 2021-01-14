package com.nextgenbroadcast.mobile.middleware.atsc3.source

import org.ngbp.libatsc3.middleware.android.phy.Atsc3NdkPHYClientBase
import org.ngbp.libatsc3.middleware.android.phy.virtual.srt.SRTRxSTLTPVirtualPHYAndroid

class SrtAtsc3Source(
        private val srtSource: String
) : BaseAtsc3Source() {
    override fun openPhyClient(): Atsc3NdkPHYClientBase {
        return SRTRxSTLTPVirtualPHYAndroid().apply {
            init()
            setSrtSourceConnectionString(srtSource)
        }.also { client ->
            client.run()
        }
    }

    override fun toString(): String {
        return "SRT Source: $srtSource"
    }
}