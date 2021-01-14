package com.nextgenbroadcast.mobile.middleware.atsc3.source

import org.ngbp.libatsc3.middleware.android.phy.Atsc3NdkPHYClientBase
import org.ngbp.libatsc3.middleware.android.phy.virtual.srt.SRTRxSTLTPVirtualPHYAndroid

class SrtListAtsc3Source(
        srtSourceList: List<String>
) : ConfigurableAtsc3Source<String>(srtSourceList) {

    override fun openPhyClient(): Atsc3NdkPHYClientBase {
        return SRTRxSTLTPVirtualPHYAndroid().apply {
            init()
            setSrtSourceConnectionString(getConfig(getConfigIndex()))
        }.also { client ->
            client.run()
        }
    }

    override fun applyConfig(config: Int): Int {
        close()
        return super.open()
    }

    override fun toString(): String {
        return "SRT list Source: ${getAllConfigs()}"
    }
}