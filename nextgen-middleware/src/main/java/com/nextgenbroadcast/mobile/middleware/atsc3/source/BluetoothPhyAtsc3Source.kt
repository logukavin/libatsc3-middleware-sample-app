package com.nextgenbroadcast.mobile.middleware.atsc3.source

import com.nextgenbroadcast.mobile.core.LOG
import org.ngbp.libatsc3.middleware.android.phy.Atsc3NdkPHYClientBase
import org.ngbp.libatsc3.middleware.android.phy.CeWiBluetoothPHYAndroid
import java.io.InputStream
import java.io.OutputStream

class BluetoothPhyAtsc3Source(
    //jjustamn-2023-06-10 - TODO: refactor me!
    private val phy: CeWiBluetoothPHYAndroid,
    private val btInputStream: InputStream,
    private val btOutputStream: OutputStream,
) : PhyAtsc3Source() {

    override fun openPhyClient(): Atsc3NdkPHYClientBase? {
        try {
            if (phy.init() == 0) {
                //jjustman-2023-06-10 - TODO: replace .open with overload method
                phy.setBluetoothInputStream(btInputStream)
                phy.setBluetoothOutputStream(btOutputStream)
//
//                if (phy.open(fd, DEVICE_TYPE_AUTO, devicePath) == 0) {
//                    if (freqKhz > 0) {
//                        phy.tune(freqKhz, 0)
//                    }
//
//                    return phy
//                }

                return phy
            }
        } catch (e: Error) {
            LOG.e(TAG, "Can't open Phy:", e)
        }

        return null
    }

    override fun toString(): String {
        return "Phy Source: phy = $phy"
    }

    companion object {
        val TAG: String = NdkPhyAtsc3Source::class.java.simpleName
    }
}