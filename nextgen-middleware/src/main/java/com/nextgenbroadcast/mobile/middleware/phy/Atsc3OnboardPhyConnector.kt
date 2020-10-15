package com.nextgenbroadcast.mobile.middleware.phy

import org.ngbp.libatsc3.middleware.android.phy.Atsc3NdkPHYClientBase

internal class Atsc3OnboardPhyConnector {

    fun <T : Atsc3NdkPHYClientBase> connect(phy: T, params: List<Triple<Int, String?, Int>>): Boolean {
        params.forEach { (fd, devicePath, freqKhz) ->
            try {
                if (phy.init() == 0) {
                    if (phy.open(fd, devicePath) == 0) {
                        if (freqKhz > 0) {
                            phy.tune(freqKhz, 0)
                        }

                        return true
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }

        return false
    }

}