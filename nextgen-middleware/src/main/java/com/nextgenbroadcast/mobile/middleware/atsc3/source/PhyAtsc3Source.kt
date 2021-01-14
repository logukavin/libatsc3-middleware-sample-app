package com.nextgenbroadcast.mobile.middleware.atsc3.source

import org.ngbp.libatsc3.middleware.android.phy.Atsc3NdkPHYClientBase

class PhyAtsc3Source(
        private val phy: Atsc3NdkPHYClientBase,
        private val fd: Int,
        private val devicePath: String?,
        private val freqKhz: Int
) : TunableConfigurableAtsc3Source() {

    override fun openPhyClient(): Atsc3NdkPHYClientBase? {
        if (phy.init() == 0) {
            if (phy.open(fd, devicePath) == 0) {
                if (freqKhz > 0) {
                    phy.tune(freqKhz, 0)
                }

                //TODO: remove? close()
                return phy
            }
        }

        return null
    }

    override fun toString(): String {
        return "Phy Source: fd = $fd, devicePath = $devicePath, freqKhz = $freqKhz"
    }
}