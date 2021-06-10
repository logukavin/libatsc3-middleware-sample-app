package com.nextgenbroadcast.mobile.middleware.atsc3

import com.nextgenbroadcast.mobile.middleware.atsc3.entities.Atsc3ServiceLocationTable
import com.nextgenbroadcast.mobile.middleware.atsc3.source.IAtsc3Source
import kotlinx.coroutines.flow.SharedFlow
import org.ngbp.libatsc3.middleware.android.phy.models.RfPhyStatistics

interface IAtsc3Module {
    val rfPhyMetricsFlow: SharedFlow<RfPhyStatistics>

    fun setListener(listener: Atsc3ModuleListener?)

    fun tune(frequencyList: List<Int>, force: Boolean)
    fun connect(source: IAtsc3Source, defaultConfig: Map<Any, Atsc3ServiceLocationTable>? = null): Boolean
    fun cancelScanning()
    fun selectAdditionalService(serviceId: Int): Boolean
    fun isServiceSelected(bsid: Int, serviceId: Int): Boolean
    fun selectService(bsid: Int, serviceId: Int): Boolean
    fun stop()
    fun close()
    fun isIdle(): Boolean

    fun getCurrentConfiguration(): Pair<String, Map<Any, Atsc3ServiceLocationTable>>?
}