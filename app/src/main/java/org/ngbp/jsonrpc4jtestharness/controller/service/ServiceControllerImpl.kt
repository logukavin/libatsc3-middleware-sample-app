package org.ngbp.jsonrpc4jtestharness.controller.service

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import org.ngbp.jsonrpc4jtestharness.core.model.ReceiverState
import org.ngbp.jsonrpc4jtestharness.core.model.SLSService
import org.ngbp.jsonrpc4jtestharness.core.repository.IRepository
import org.ngbp.libatsc3.Atsc3Module
import org.ngbp.libatsc3.entities.app.Atsc3Application
import org.ngbp.libatsc3.entities.held.Atsc3HeldPackage
import org.ngbp.libatsc3.entities.service.Atsc3Service
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceControllerImpl @Inject constructor(
        private val repository: IRepository,
        private val atsc3Module: Atsc3Module
) : IServiceController, Atsc3Module.Listener {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    private var heldResetJob: Job? = null
    private var mediaUrlAssignmentJob: Job? = null

    override val receiverState = MutableLiveData<ReceiverState>()

    override val selectedService = repository.selectedService
    override val serviceGuidUrls = repository.serviceGuideUrls

    override val sltServices = repository.services

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

    override fun onPackageReceived(appPackage: Atsc3Application) {
        repository.addOrUpdateApplication(appPackage)
    }

    override fun onCurrentServicePackageChanged(pkg: Atsc3HeldPackage?) {
        cancelHeldReset()

        repository.setHeldPackage(pkg)
    }

    override fun onCurrentServiceDashPatched(mpdPath: String) {
        setMediaUrlWithDelay(mpdPath)
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
        // Reset current media. New media url will be received after service selection.
        cancelMediaUrlAssignment()
        repository.setMediaUrl(null)

        val res = atsc3Module.selectService(service.id)
        if (res) {
            // Store successfully selected service. This will lead to RMP reset
            repository.setSelectedService(service)

            // Reset the current HELD if it's not compatible new service or start delayed reset otherwise.
            // Delayed reset will be canceled when a new HELD been received for selected service.
            repository.heldPackage.value?.let { currentHeld ->
                // Is new service compatible with current HELD?
                if (currentHeld.coupledServices?.contains(service.id) == true) {
                    resetHeldWithDelay()
                } else {
                    repository.setHeldPackage(null)
                }
            }
        } else {
            // Reset HELD and service if service can't be selected
            repository.setHeldPackage(null)
            repository.setSelectedService(null)
        }
    }

    private fun resetHeldWithDelay() {
        cancelHeldReset()
        heldResetJob = ioScope.launch {
            delay(BA_LOADING_TIMEOUT)
            withContext(Dispatchers.Main) {
                repository.setHeldPackage(null)
                heldResetJob = null
            }
        }
    }

    private fun cancelHeldReset() {
        heldResetJob?.let {
            it.cancel()
            heldResetJob = null
        }
    }

    private fun setMediaUrlWithDelay(mpdPath: String) {
        cancelMediaUrlAssignment()
        mediaUrlAssignmentJob = ioScope.launch {
            delay(MPD_UPDATE_DELAY)
            withContext(Dispatchers.Main) {
                repository.setMediaUrl(mpdPath)
                mediaUrlAssignmentJob = null
            }
        }
    }

    private fun cancelMediaUrlAssignment() {
        mediaUrlAssignmentJob?.let {
            it.cancel()
            mediaUrlAssignmentJob = null
        }
    }

    companion object {
        private const val BA_LOADING_TIMEOUT = 5000L
        private const val MPD_UPDATE_DELAY = 2000L
    }
}