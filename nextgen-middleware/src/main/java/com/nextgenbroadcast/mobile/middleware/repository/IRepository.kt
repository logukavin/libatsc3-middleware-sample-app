package com.nextgenbroadcast.mobile.middleware.repository

import android.location.Location
import com.nextgenbroadcast.mobile.core.atsc3.MediaUrl
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts.AeaTable
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl
import kotlinx.coroutines.flow.StateFlow

internal interface IRepository {
    // Receiver
    val selectedService: StateFlow<AVService?>
    val serviceGuideUrls: StateFlow<List<SGUrl>>
    val alertsForNotify: StateFlow<List<AeaTable>>

    // Media Player
    val routeMediaUrl: StateFlow<MediaUrl?>

    // User Agent
    val applications: StateFlow<List<Atsc3Application>>
    val services: StateFlow<List<AVService>>
    val heldPackage: StateFlow<Atsc3HeldPackage?>

    val lastLocation: StateFlow<Location?>

    fun addOrUpdateApplication(application: Atsc3Application)
    fun findApplication(appContextId: String): Atsc3Application?

    fun setServices(services: List<AVService>)
    fun setSelectedService(service: AVService?)
    fun findServiceBy(globalServiceId: String): AVService?
    fun findServiceBy(bsid: Int, serviceId: Int): AVService?
    fun findServiceOrNull(predicate: (AVService) -> Boolean): AVService?

    fun setHeldPackage(data: Atsc3HeldPackage?)
    fun setMediaUrl(mediaUrl: MediaUrl?)

    fun setAlertList(newAlerts: List<AeaTable>)

    fun updateLastLocation(location: Location?)

    fun reset()
}