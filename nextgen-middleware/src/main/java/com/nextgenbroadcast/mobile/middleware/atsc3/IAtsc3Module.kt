package com.nextgenbroadcast.mobile.middleware.atsc3

import com.nextgenbroadcast.mobile.middleware.atsc3.source.IAtsc3Source
import org.ngbp.libatsc3.middleware.android.application.interfaces.IAtsc3NdkApplicationBridgeCallbacks
import org.ngbp.libatsc3.middleware.android.phy.interfaces.IAtsc3NdkPHYBridgeCallbacks

interface IAtsc3Module: IAtsc3NdkApplicationBridgeCallbacks, IAtsc3NdkPHYBridgeCallbacks {

    fun setListener(listener: Atsc3Module.Listener?)
    fun tune(freqKhz: Int, frequencies: List<Int>, retuneOnDemod: Boolean)
    fun connect(source: IAtsc3Source): Boolean
    fun selectAdditionalService(serviceId: Int): Boolean
    fun selectService(bsid: Int, serviceId: Int): Boolean
    fun stop()
    fun close()

}