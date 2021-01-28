package com.nextgenbroadcast.mobile.middleware.repository

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.serviceGuide.SGScheduleMap
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl

//TODO: get rid of LiveData here
internal interface IRepository {
    // Receiver
    val selectedService: LiveData<AVService?>
    val serviceSchedule: LiveData<SGScheduleMap?>
    val serviceGuideUrls: LiveData<List<SGUrl>?>

    // Media Player
    val routeMediaUrl: LiveData<String?>

    // User Agent
    val applications: LiveData<List<Atsc3Application>?>
    val services: LiveData<List<AVService>>
    val heldPackage: LiveData<Atsc3HeldPackage?>

    fun addOrUpdateApplication(application: Atsc3Application)
    fun findApplication(appContextId: String): Atsc3Application?

    fun setServices(services: List<AVService>)
    fun setServiceSchedule(schedule: SGScheduleMap)
    fun setServiceGuideUrls(services: List<SGUrl>)
    fun setSelectedService(service: AVService?)
    fun findServiceById(globalServiceId: String): AVService?

    fun setHeldPackage(data: Atsc3HeldPackage?)
    fun setMediaUrl(mediaUrl: String?)

    fun reset()
}