package com.nextgenbroadcast.mobile.middleware.repository

import androidx.lifecycle.MutableLiveData
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.serviceGuide.SGScheduleMap
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl
import java.util.concurrent.ConcurrentHashMap

internal class RepositoryImpl : IRepository {
    private val _applications = ConcurrentHashMap<String, Atsc3Application>()

    override val selectedService = MutableLiveData<AVService>()
    override val serviceSchedule = MutableLiveData<SGScheduleMap>()
    override val serviceGuideUrls = MutableLiveData<List<SGUrl>>()

    override val routeMediaUrl = MutableLiveData<String>()

    override val applications = MutableLiveData<List<Atsc3Application>?>()
    override val services = MutableLiveData<List<AVService>>()
    override val heldPackage = MutableLiveData<Atsc3HeldPackage?>()

    override fun addOrUpdateApplication(application: Atsc3Application) {
        _applications[application.uid] = application
        applications.postValue(_applications.values.toList())
    }

    override fun findApplication(appContextId: String): Atsc3Application? {
        return _applications.elements().toList().firstOrNull { app ->
            app.appContextIdList.contains(appContextId)
        }
    }

    override fun setServices(services: List<AVService>) {
        this.services.postValue(services)
    }

    override fun setServiceSchedule(schedule: SGScheduleMap) {
        serviceSchedule.postValue(schedule)
    }

    override fun setServiceGuideUrls(services: List<SGUrl>) {
        serviceGuideUrls.postValue(services)
    }

    override fun setSelectedService(service: AVService?) {
        selectedService.postValue(service)
    }

    override fun setHeldPackage(data: Atsc3HeldPackage?) {
        heldPackage.postValue(data)
    }

    override fun setMediaUrl(mediaUrl: String?) {
        routeMediaUrl.postValue(mediaUrl)
    }

    override fun reset() {
        selectedService.postValue(null)
        serviceSchedule.postValue(emptyMap())
        serviceGuideUrls.postValue(emptyList())
        services.postValue(emptyList())
        heldPackage.postValue(null)
        routeMediaUrl.postValue(null)
    }
}