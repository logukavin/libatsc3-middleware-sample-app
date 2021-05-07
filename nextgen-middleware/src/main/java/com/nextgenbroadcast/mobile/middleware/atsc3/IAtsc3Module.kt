package com.nextgenbroadcast.mobile.middleware.atsc3

import com.nextgenbroadcast.mobile.middleware.atsc3.source.IAtsc3Source
import kotlinx.coroutines.flow.SharedFlow
import org.ngbp.libatsc3.middleware.android.phy.models.RfPhyStatistics

interface IAtsc3Module {
    val rfPhyMetricsFlow: SharedFlow<RfPhyStatistics>

    fun setListener(listener: Atsc3ModuleListener?)
    fun tune(freqKhz: Int, frequencies: List<Int>, retuneOnDemod: Boolean)
    fun connect(source: IAtsc3Source): Boolean
    fun selectAdditionalService(serviceId: Int): Boolean
    fun isServiceSelected(bsid: Int, serviceId: Int): Boolean
    fun selectService(bsid: Int, serviceId: Int): Boolean
    fun stop()
    fun close()
    fun isIdle(): Boolean
}