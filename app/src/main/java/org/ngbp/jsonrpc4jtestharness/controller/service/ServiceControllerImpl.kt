package org.ngbp.jsonrpc4jtestharness.controller.service

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import org.ngbp.jsonrpc4jtestharness.core.model.AppData
import org.ngbp.jsonrpc4jtestharness.core.model.ReceiverState
import org.ngbp.jsonrpc4jtestharness.core.model.SLSService
import org.ngbp.jsonrpc4jtestharness.core.repository.IRepository
import org.ngbp.libatsc3.Atsc3Module
import org.ngbp.libatsc3.entities.held.Atsc3HeldPackage
import org.ngbp.libatsc3.entities.service.Atsc3Service
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceControllerImpl @Inject constructor(
        private val repository: IRepository,
        private val atsc3Module: Atsc3Module
) : IServiceController, Atsc3Module.Listener {

    private var unloadBAJob: Job? = null

    override val receiverState = MutableLiveData<ReceiverState>()

    override val selectedService = repository.selectedService
    override val serviceGuidUrls = repository.serviceGuideUrls

    override val sltServices = repository.availableServices

    init {
        atsc3Module.setListener(this)
    }

    override fun onStateChanged(state: Atsc3Module.State?) {
        val newState = state?.let { ReceiverState.valueOf(state.name) } ?: ReceiverState.IDLE

        receiverState.postValue(newState)

        if (newState == ReceiverState.IDLE) {
            repository.reset()
        }
    }

    override fun onServicesLoaded(services: List<Atsc3Service?>) {
        val slsServices = services.filterNotNull()
                .map { SLSService(it.serviceId, it.shortServiceName, it.globalServiceId) }
        repository.setServices(slsServices)
    }

    override fun onCurrentServicePackageChanged(pkg: Atsc3HeldPackage?) {
        cancelUnloadBAJob()

        val newAppData = pkg?.let {
            val appContextId = it.appContextId ?: return@let null
            val appEntryPage = it.bcastEntryPageUrl ?: it.bbandEntryPageUrl ?: return@let null
            val compatibleServiceIds = it.coupledServices ?: emptyList()
            AppData(appContextId, appEntryPage, compatibleServiceIds)
        }

        repository.setAppEntryPoint(newAppData)
    }

    override fun openRoute(pcapFile: String): Boolean {
        return atsc3Module.openPcapFile(pcapFile)
    }

    override fun stopRoute() {
        atsc3Module.stop()
    }

    override fun closeRoute() {
        atsc3Module.close()
    }

    override fun selectService(service: SLSService) {
        val res = atsc3Module.selectService(service.id)
        if (res) {
            // this will reset RMP
            repository.setSelectedService(service)

            // force reset BA if it's not compatible with current or start delayed reset
            repository.appData.value?.let { currentApp ->
                if (!currentApp.compatibleServiceIds.contains(service.id)) {
                    repository.setAppEntryPoint(null)
                } else {
                    startUnloadBAJob()
                }
            }

            //TODO: temporary waiter for media. Should use notification instead
            GlobalScope.launch {
                var done = false
                var iterator = 5
                while (!done && iterator > 0) {
                    iterator--
                    delay(200)

                    done = withContext(Dispatchers.Main) {
                        val media = getServiceMediaUrl()
                        if (media != null) {
                            repository.setMediaUrl(media)
                        }

                        return@withContext (media != null)
                    }
                }
            }
        } else {
            repository.setAppEntryPoint(null)
            repository.setSelectedService(null)
        }
    }

    private fun getServiceMediaUrl() = atsc3Module.getSelectedServiceMediaUri()

    private fun startUnloadBAJob() {
        cancelUnloadBAJob()
        unloadBAJob = GlobalScope.launch {
            delay(BA_LOADING_TIMEOUT)
            withContext(Dispatchers.Main) {
                repository.setAppEntryPoint(null)
                unloadBAJob = null
            }
        }
    }

    private fun cancelUnloadBAJob() {
        unloadBAJob?.let {
            it.cancel()
            unloadBAJob = null
        }
    }

    companion object {
        private const val BA_LOADING_TIMEOUT = 5000L
    }
}