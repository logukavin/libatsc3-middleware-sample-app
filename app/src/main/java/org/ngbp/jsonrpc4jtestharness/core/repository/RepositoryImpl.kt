package org.ngbp.jsonrpc4jtestharness.core.repository

import androidx.lifecycle.MutableLiveData
import org.ngbp.jsonrpc4jtestharness.core.model.SLSService
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Urls
import org.ngbp.libatsc3.entities.app.Atsc3Application
import org.ngbp.libatsc3.entities.held.Atsc3HeldPackage
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositoryImpl @Inject constructor() : IRepository {
    private val _applications = ConcurrentHashMap<String, Atsc3Application>()

    override val selectedService = MutableLiveData<SLSService>()
    override val serviceGuideUrls = MutableLiveData<List<Urls>>()

    override val routeMediaUrl = MutableLiveData<String>()

    override val applications = MutableLiveData<List<Atsc3Application>>()
    override val services = MutableLiveData<List<SLSService>>()
    override val heldPackage = MutableLiveData<Atsc3HeldPackage?>()

    init {
        //TODO: remove after tests
        selectedService.value = SLSService(5003, "WZTV", "tag:sinclairplatform.com,2020:WZTV:2727")
    }

    override fun addOrUpdateApplication(application: Atsc3Application) {
        _applications[application.cachePath] = application
        applications.postValue(_applications.values.toList())
    }

    override fun findApplication(appContextId: String): Atsc3Application? {
        return _applications.elements().toList().firstOrNull { app ->
            app.appContextIdList.contains(appContextId)
        }
    }

    override fun setServices(services: List<SLSService>) {
        this.services.postValue(services)
    }

    override fun setSelectedService(service: SLSService?) {
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
        serviceGuideUrls.postValue(emptyList())
        services.postValue(emptyList())
        heldPackage.postValue(null)
        routeMediaUrl.postValue(null)
    }
}