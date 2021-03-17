package com.nextgenbroadcast.mobile.middleware.repository

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.atsc3.MediaUrl
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts.AeaTable
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl

//TODO: get rid of LiveData here
internal interface IRepository {
    // Receiver
    val selectedService: LiveData<AVService?>
    val serviceGuideUrls: LiveData<List<SGUrl>?>

    // Media Player
    val routeMediaUrl: LiveData<MediaUrl?>

    // User Agent
    val applications: LiveData<List<Atsc3Application>?>
    val services: LiveData<List<AVService>>
    val heldPackage: LiveData<Atsc3HeldPackage?>

    val alertsForNotify: LiveData<List<AeaTable>>
    val mergedAlerts: MutableList<AeaTable>

    fun addOrUpdateApplication(application: Atsc3Application)
    fun findApplication(appContextId: String): Atsc3Application?

    fun setServices(services: List<AVService>)
    fun setSelectedService(service: AVService?)
    fun findServiceBy(globalServiceId: String): AVService?
    fun findServiceBy(bsid: Int, serviceId: Int): AVService?

    fun setHeldPackage(data: Atsc3HeldPackage?)
    fun setMediaUrl(mediaUrl: MediaUrl?)

    fun storeAlertsAndNotify(list: List<AeaTable>)

    fun reset()
}