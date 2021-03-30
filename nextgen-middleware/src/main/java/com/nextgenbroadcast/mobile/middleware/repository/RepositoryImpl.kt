package com.nextgenbroadcast.mobile.middleware.repository

import com.nextgenbroadcast.mobile.core.atsc3.MediaUrl
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts.AeaTable
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap

internal class RepositoryImpl : IRepository {
    private val _applications = ConcurrentHashMap<String, Atsc3Application>()

    override val selectedService = MutableStateFlow<AVService?>(null)
    override val serviceGuideUrls = MutableStateFlow<List<SGUrl>>(emptyList())
    override val alertsForNotify = MutableStateFlow<List<AeaTable>>(emptyList())

    override val routeMediaUrl = MutableStateFlow<MediaUrl?>(null)

    override val applications = MutableStateFlow<List<Atsc3Application>>(emptyList())
    override val services = MutableStateFlow<List<AVService>>(emptyList())
    override val heldPackage = MutableStateFlow<Atsc3HeldPackage?>(null)

    override fun addOrUpdateApplication(application: Atsc3Application) {
        _applications[application.uid] = application
        applications.value = _applications.values.toList()
    }

    override fun findApplication(appContextId: String): Atsc3Application? {
        return _applications.elements().toList().firstOrNull { app ->
            app.appContextIdList.contains(appContextId)
        }
    }

    override fun setServices(services: List<AVService>) {
        this.services.value = services
    }

    override fun setSelectedService(service: AVService?) {
        selectedService.value = service
    }

    override fun findServiceBy(globalServiceId: String): AVService? {
        return services.value.firstOrNull { it.globalId == globalServiceId }
    }

    override fun findServiceBy(bsid: Int, serviceId: Int): AVService? {
        return services.value.firstOrNull { it.bsid == bsid && it.id == serviceId }
    }

    override fun setHeldPackage(data: Atsc3HeldPackage?) {
        heldPackage.value = data
    }

    override fun setMediaUrl(mediaUrl: MediaUrl?) {
        routeMediaUrl.value = mediaUrl
    }

    override fun setAlertList(newAlerts: List<AeaTable>) {
        val currentAlerts = alertsForNotify.value.toMutableList()

        newAlerts.forEach { aea ->
            if (aea.type == AeaTable.CANCEL_ALERT) {
                currentAlerts.removeIf { it.id == aea.refId }
            } else {
                //TODO filter aea by expires date
                currentAlerts.add(aea)
            }
        }
        alertsForNotify.value = currentAlerts
    }

    override fun reset() {
        selectedService.value = null
        serviceGuideUrls.value = emptyList()
        services.value = emptyList()
        heldPackage.value = null
        routeMediaUrl.value = null
        alertsForNotify.value = emptyList()
    }
}