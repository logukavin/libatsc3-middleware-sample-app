package com.nextgenbroadcast.mobile.middleware.repository

import androidx.lifecycle.MutableLiveData
import com.nextgenbroadcast.mobile.core.atsc3.MediaUrl
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts.AeaTable
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl
import java.util.concurrent.ConcurrentHashMap

//TODO: we should avoid using LiveData here
internal class RepositoryImpl : IRepository {
    private val _applications = ConcurrentHashMap<String, Atsc3Application>()

    override val selectedService = MutableLiveData<AVService>()
    override val serviceGuideUrls = MutableLiveData<List<SGUrl>>()

    override val routeMediaUrl = MutableLiveData<MediaUrl>()

    override val applications = MutableLiveData<List<Atsc3Application>?>()
    override val services = MutableLiveData<List<AVService>>()
    override val heldPackage = MutableLiveData<Atsc3HeldPackage?>()
    override val alertsForNotify = MutableLiveData<List<AeaTable>>()
    override val storedAlerts = mutableListOf<AeaTable>()

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

    override fun setSelectedService(service: AVService?) {
        selectedService.postValue(service)
    }

    override fun findServiceBy(globalServiceId: String): AVService? {
        return services.value?.let { list ->
            list.firstOrNull { it.globalId == globalServiceId }
        }
    }

    override fun findServiceBy(bsid: Int, serviceId: Int): AVService? {
        return services.value?.let { list ->
            list.firstOrNull { it.bsid == bsid && it.id == serviceId }
        }
    }

    override fun setHeldPackage(data: Atsc3HeldPackage?) {
        heldPackage.postValue(data)
    }

    override fun setMediaUrl(mediaUrl: MediaUrl?) {
        routeMediaUrl.postValue(mediaUrl)
    }

    override fun storeAlertsAndNotify(list: List<AeaTable>) {
        val notifyAlerts = mutableListOf<AeaTable>()

        list.forEach { aea ->
            when (aea.type) {
                AeaTable.CANCEL_ALERT -> {
                    val removed = storedAlerts.removeIf { it.id == aea.refId }
                    if (!removed) notifyAlerts.add(aea)
                }
                else -> {
                    //TODO filter aea by expires date
                    notifyAlerts.add(aea)
                }
            }
        }

        alertsForNotify.postValue(notifyAlerts)
        storedAlerts.addAll(notifyAlerts)
        //TODO storedAlerts.removeIf { it.expires < currentDate }
    }

    override fun reset() {
        selectedService.postValue(null)
        serviceGuideUrls.postValue(emptyList())
        services.postValue(emptyList())
        heldPackage.postValue(null)
        routeMediaUrl.postValue(null)
    }
}