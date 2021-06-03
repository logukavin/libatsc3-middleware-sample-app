package com.nextgenbroadcast.mobile.middleware.atsc3.source

import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3Module
import org.ngbp.libatsc3.middleware.android.phy.Atsc3NdkPHYClientBase
import org.ngbp.libatsc3.middleware.android.phy.virtual.PcapDemuxedVirtualPHYAndroid
import org.ngbp.libatsc3.middleware.android.phy.virtual.PcapSTLTPVirtualPHYAndroid

class PcapAtsc3Source(
        private val filename: String
) : Atsc3Source() {

    enum class PcapType {
        DEMUXED, STLTP
    }

    override fun openPhyClient(): Atsc3NdkPHYClientBase? {
        //TODO: temporary solution
        val type = if (filename.contains(".demux.")) PcapType.DEMUXED else PcapType.STLTP
        try {
            when (type) {
                PcapType.DEMUXED -> PcapDemuxedVirtualPHYAndroid()
                PcapType.STLTP -> PcapSTLTPVirtualPHYAndroid()
            }.apply {
                init()
            }.also { client ->
                val res = client.open_from_capture(filename)

                //TODO: for assets mAt3DrvIntf.atsc3_pcap_open_for_replay_from_assetManager(filename, assetManager);
                if (res == Atsc3Module.RES_OK) {
                    client.run()
                    return client
                }
            }
        } catch (e: Error) {
            LOG.e(TAG, "Can't open Pcap file: $filename, type: $type", e)
        }

        return null
    }

    override fun getConfigCount() = 1

    override fun getConfigByIndex(configIndex: Int): String {
        return if (configIndex == 0) {
            filename
        } else {
            throw IndexOutOfBoundsException("Incorrect configuration index: $configIndex")
        }
    }

    override fun getAllConfigs(): List<String> = listOf(filename)

    override fun toString(): String {
        return "PCAP Source: filename = $filename"
    }

    companion object {
        val TAG: String = PcapAtsc3Source::class.java.simpleName
    }
}