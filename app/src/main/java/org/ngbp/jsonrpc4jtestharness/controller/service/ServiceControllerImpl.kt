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

    override val receiverState = MutableLiveData<ReceiverState>()

    override val selectedService = repository.selectedService

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
        //TODO: fire load/unload events instead
        val data = pkg?.let {
            AppData(it.appContextId, it.bcastEntryPageUrl ?: it.bbandEntryPageUrl)
        }
        repository.setAppEntryPoint(data)
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
            //TODO: should we use globalId or context from HELD?
            repository.setSelectedService(service)

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

            repository.setAppEntryPoint(AppData(
                    atsc3Module.getSelectedServiceAppContextId(),
                    atsc3Module.getSelectedServiceEntryPoint()
            ))
        } else {
            repository.setAppEntryPoint(null)
        }
    }

    private fun getServiceMediaUrl() = atsc3Module.getSelectedServiceMediaUri()
}