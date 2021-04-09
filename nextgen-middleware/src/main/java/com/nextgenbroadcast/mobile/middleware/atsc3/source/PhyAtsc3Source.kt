package com.nextgenbroadcast.mobile.middleware.atsc3.source

import com.nextgenbroadcast.mobile.core.LOG
import org.ngbp.libatsc3.middleware.android.phy.Atsc3NdkPHYClientBase

class PhyAtsc3Source(
        private val phy: Atsc3NdkPHYClientBase,
        private val fd: Int,
        private val devicePath: String?,
        private val freqKhz: Int
) : TunableConfigurableAtsc3Source() {

    override fun openPhyClient(): Atsc3NdkPHYClientBase? {
        try {
            if (phy.init() == 0) {
                if (phy.open(fd, devicePath) == 0) {
                    phy.startPhyOpenTrace()

                    if (freqKhz > 0) {
                        phy.tune(freqKhz, 0)
                    }

                    return phy
                }
            }
        } catch (e: Error) {
            LOG.e(TAG, "Can't open Phy: fd:$fd, devicePath: $devicePath, freqKhz:$freqKhz", e)
        }

        return null
    }

    override fun close() {
        super.close()

        phy.stopPhyOpenTrace()
        phy.stopPhyTunedTrace()
    }

    override fun toString(): String {
        return "Phy Source: fd = $fd, devicePath = $devicePath, freqKhz = $freqKhz"
    }

    companion object {
        val TAG: String = PhyAtsc3Source::class.java.simpleName
    }
}