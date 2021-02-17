package com.nextgenbroadcast.mobile.middleware.atsc3

import com.nextgenbroadcast.mobile.middleware.atsc3.source.IAtsc3Source
import org.ngbp.libatsc3.middleware.android.application.interfaces.IAtsc3NdkApplicationBridgeCallbacks
import org.ngbp.libatsc3.middleware.android.phy.interfaces.IAtsc3NdkPHYBridgeCallbacks

interface IAtsc3Module {

    fun setListener(listener: Atsc3ModuleListener?)
    fun tune(freqKhz: Int, frequencies: List<Int>, retuneOnDemod: Boolean)
    fun connect(source: IAtsc3Source): Boolean
    fun selectAdditionalService(serviceId: Int): Boolean
    fun selectService(bsid: Int, serviceId: Int): Boolean
    fun stop()
    fun close()

}