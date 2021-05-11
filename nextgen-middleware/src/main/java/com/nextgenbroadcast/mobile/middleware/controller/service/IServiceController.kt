package com.nextgenbroadcast.mobile.middleware.controller.service

import com.nextgenbroadcast.mobile.core.atsc3.MediaUrl
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts.AeaTable
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.source.IAtsc3Source
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl
import kotlinx.coroutines.flow.StateFlow

internal interface IServiceController {
    val receiverState: StateFlow<ReceiverState>
    val receiverFrequency: StateFlow<Int>
    val routeServices: StateFlow<List<AVService>>
    val selectedService: StateFlow<AVService?>
    val serviceGuideUrls: StateFlow<List<SGUrl>>
    val applications: StateFlow<List<Atsc3Application>>
    val alertList: StateFlow<List<AeaTable>>

    suspend fun openRoute(source: IAtsc3Source, force: Boolean = false): Boolean
    suspend fun stopRoute()
    suspend fun closeRoute()
    suspend fun tune(frequency: PhyFrequency)
    suspend fun selectService(service: AVService): Boolean

    fun findServiceById(globalServiceId: String): AVService?
    fun getNearbyService(offset: Int): AVService?
    fun getCurrentService(): AVService?
    fun getCurrentRouteMediaUrl(): MediaUrl?
}