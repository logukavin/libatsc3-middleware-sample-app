package com.nextgenbroadcast.mobile.middleware.atsc3.source

import com.nextgenbroadcast.mobile.core.LOG
import org.ngbp.libatsc3.middleware.android.phy.Atsc3NdkPHYClientBase
import org.ngbp.libatsc3.middleware.android.phy.virtual.srt.SRTRxSTLTPVirtualPHYAndroid

class SrtAtsc3Source(
        private val srtSource: String
) : BaseAtsc3Source() {
    override fun openPhyClient(): Atsc3NdkPHYClientBase? {
        try {
            return SRTRxSTLTPVirtualPHYAndroid().apply {
                init()
                setSrtSourceConnectionString(srtSource)
            }.also { client ->
                client.run()
            }
        } catch (e: Error) {
            LOG.e(TAG, "Can't open SRT stream: $srtSource", e)
        }

        return null
    }

    override fun toString(): String {
        return "SRT Source: $srtSource"
    }

    companion object {
        val TAG: String = SrtAtsc3Source::class.java.simpleName
    }
}