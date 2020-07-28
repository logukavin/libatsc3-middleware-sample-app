package org.ngbp.jsonrpc4jtestharness.core.repository

import androidx.lifecycle.MutableLiveData
import org.ngbp.jsonrpc4jtestharness.core.model.AppData
import org.ngbp.jsonrpc4jtestharness.core.model.SLSService
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Urls
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositoryImpl @Inject constructor() : IRepository {
    override val selectedService = MutableLiveData<SLSService>()
    override val serviceGuideUrls = MutableLiveData<List<Urls>>()

    override val routeMediaUrl = MutableLiveData<String>()

    override val availableServices = MutableLiveData<List<SLSService>>()
    override val appData = MutableLiveData<AppData?>()

    init {
        //TODO: remove after tests
        selectedService.value = SLSService(5003, "WZTV", "tag:sinclairplatform.com,2020:WZTV:2727")
    }

    override fun setServices(services: List<SLSService>) {
        availableServices.postValue(services)
    }

    override fun setSelectedService(service: SLSService?) {
        selectedService.postValue(service)
    }

    override fun setAppEntryPoint(data: AppData?) {
        appData.postValue(data)
    }

    override fun setMediaUrl(mediaUrl: String?) {
        routeMediaUrl.postValue(mediaUrl)
    }

    override fun reset() {
        selectedService.postValue(null)
        serviceGuideUrls.postValue(emptyList())
        availableServices.postValue(emptyList())
        appData.postValue(null)
        routeMediaUrl.postValue(null)
    }
}