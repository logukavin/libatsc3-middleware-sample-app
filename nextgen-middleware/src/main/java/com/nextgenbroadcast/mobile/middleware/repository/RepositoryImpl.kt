package com.nextgenbroadcast.mobile.middleware.repository

import android.location.Location
import android.util.Log
import com.nextgenbroadcast.mobile.core.model.MediaUrl
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts.AeaTable
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl
import com.nextgenbroadcast.mobile.middleware.server.ServerUtils
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

internal class RepositoryImpl(
    private val settings: IMiddlewareSettings
) : IRepository {
    private val _applications = ConcurrentHashMap<String, Atsc3Application>()
    private val sessionNum = MutableStateFlow(0)

    override val selectedService = MutableStateFlow<AVService?>(null)
    override val serviceGuideUrls = MutableStateFlow<List<SGUrl>>(emptyList())
    override val alertsForNotify = MutableStateFlow<List<AeaTable>>(emptyList())

    override val routeMediaUrl = MutableStateFlow<MediaUrl?>(null)

    override val applications = MutableStateFlow<List<Atsc3Application>>(emptyList())
    override val services = MutableStateFlow<List<AVService>>(emptyList())
    override val heldPackage = MutableStateFlow<Atsc3HeldPackage?>(null)
    override val appData = combine(heldPackage, applications, sessionNum) { held, applications, _ ->
        held?.let {
            val appContextId = held.appContextId ?: return@let null
            val appUrl = held.bcastEntryPageUrl?.let { entryPageUrl ->
                ServerUtils.createEntryPoint(entryPageUrl, appContextId, settings)
            } ?: held.bbandEntryPageUrl ?: return@let null
            val compatibleServiceIds = held.coupledServices ?: emptyList()
            val application = applications.firstOrNull { app ->
                app.appContextIdList.contains(appContextId) && app.packageName == held.bcastEntryPackageUrl
            }

            AppData(
                appContextId,
                ServerUtils.addSocketPath(appUrl, settings),
                compatibleServiceIds,
                application?.cachePath
            )
        }
    }

    override val lastLocation = MutableStateFlow<Location?>(null)

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

    override fun findServiceOrNull(predicate: (AVService) -> Boolean): AVService? {
        return services.value.firstOrNull(predicate)
    }

    override fun setHeldPackage(data: Atsc3HeldPackage?) {
        heldPackage.value = data
    }

    override fun setMediaUrl(mediaUrl: MediaUrl?) {
        Log.d(TAG, "setMediaUrl: $mediaUrl")
        routeMediaUrl.value = mediaUrl
    }

    override fun setAlertList(newAlerts: List<AeaTable>) {
        val currentAlerts = alertsForNotify.value.associateBy({ it.id }, { it }).toMutableMap()
        val canceledIds: MutableSet<String> = mutableSetOf()
        val currentTime = ZonedDateTime.now()

        newAlerts.forEach { aea ->
            if (aea.type == AeaTable.CANCEL_ALERT) {
                aea.refId?.let {
                    canceledIds.add(it)
                }
            } else {
                currentAlerts[aea.id] = aea
            }
        }

        currentAlerts.values.removeIf {
            isExpired(it.expires, currentTime) || canceledIds.contains(it.id)
        }

        alertsForNotify.value = currentAlerts.values.toMutableList()
    }

    override fun updateLastLocation(location: Location?) {
        lastLocation.value = location
    }

    private fun isExpired(expireTime: ZonedDateTime?, currentTime: ZonedDateTime): Boolean {
        if (expireTime == null) return true
        return expireTime.isBefore(currentTime)
    }

    override fun reset() {
        selectedService.value = null
        serviceGuideUrls.value = emptyList()
        services.value = emptyList()
        heldPackage.value = null
        routeMediaUrl.value = null
        alertsForNotify.value = emptyList()
    }

    override fun onNewSessionStarted() {
        sessionNum.value++
    }

    companion object {
        val TAG: String = RepositoryImpl::class.java.simpleName
    }
}