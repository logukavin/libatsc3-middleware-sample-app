package com.nextgenbroadcast.mobile.middleware.controller.service

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.middleware.analytics.IAtsc3Analytics
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3Module
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.SLTConstants
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.service.Atsc3Service
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.ServiceGuideStore
import com.nextgenbroadcast.mobile.middleware.atsc3.source.*
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import kotlinx.coroutines.*


internal class ServiceControllerImpl (
        private val repository: IRepository,
        private val settings: IMiddlewareSettings,
        private val atsc3Module: Atsc3Module,
        private val atsc3Analytics: IAtsc3Analytics
) : IServiceController, Atsc3Module.Listener {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val serviceGuideStore = ServiceGuideStore(repository, settings)

    private var heldResetJob: Job? = null
    private var mediaUrlAssignmentJob: Job? = null

    override val receiverState = MutableLiveData(ReceiverState.IDLE)

    override val selectedService = repository.selectedService
    override val serviceGuidUrls = repository.serviceGuideUrls

    override val sltServices = repository.services

    override val freqKhz = MutableLiveData(0)

    override val schedule = repository.serviceSchedule

    init {
        atsc3Module.setListener(this)
    }

    override fun onStateChanged(state: Atsc3Module.State) {
        //TODO: rewrite this mapping
        val newState = ReceiverState.valueOf(state.name)

        receiverState.postValue(newState)

        if (newState == ReceiverState.IDLE) {
            repository.reset()
        }
    }

    override fun onServiceLocationTableChanged(services: List<Atsc3Service>, reportServerUrl: String?) {
        atsc3Analytics.setReportServerUrl(reportServerUrl)

        // store A/V services
        val avServices = services.filter { service ->
            service.serviceCategory == SLTConstants.SERVICE_CATEGORY_AV
                    || service.serviceCategory == SLTConstants.SERVICE_CATEGORY_AO
        }.map {
            AVService(
                    it.bsid,
                    it.serviceId,
                    it.shortServiceName,
                    it.globalServiceId,
                    it.majorChannelNo,
                    it.minorChannelNo,
                    it.serviceCategory
            )
        }
        repository.setServices(avServices)

        // select ESG service
        services.firstOrNull { service ->
            service.serviceCategory == SLTConstants.SERVICE_CATEGORY_ESG
        }?.let { service ->
            atsc3Module.selectAdditionalService(service.serviceId)
        }
    }

    override fun onPackageReceived(appPackage: Atsc3Application) {
        Log.d("ServiceControllerImpl", "onPackageReceived - appPackage: $appPackage")

        repository.addOrUpdateApplication(appPackage)
    }

    override fun onCurrentServicePackageChanged(pkg: Atsc3HeldPackage?) {
        cancelHeldReset()

        repository.setHeldPackage(pkg)
    }

    override fun onCurrentServiceDashPatched(mpdPath: String) {
        setMediaUrlWithDelay(mpdPath)
    }

    override fun onServiceGuideUnitReceived(filePath: String) {
        serviceGuideStore.readDeliveryUnit(filePath)
    }

    override fun openRoute(path: String): Boolean {
        val source = if (path.startsWith("srt://")) {
            if (path.contains('\n')) {
                val sources = path.split('\n')
                SrtListAtsc3Source(sources)
            } else {
                SrtAtsc3Source(path)
            }
        } else {
            //TODO: temporary solution
            val type = if (path.contains(".demux.")) PcapAtsc3Source.PcapType.DEMUXED else PcapAtsc3Source.PcapType.STLTP
            PcapAtsc3Source(path, type)
        }

        return openRoute(source)
    }

    override fun openRoute(source: IAtsc3Source): Boolean {
        closeRoute()

        if (atsc3Module.connect(source)) {
            if (source is ITunableSource) {
                tune(PhyFrequency.default(PhyFrequency.Source.AUTO))
            }
            return true
        }
        return false
    }

    override fun stopRoute() {
        atsc3Module.stop()
    }

    override fun closeRoute() {
        atsc3Analytics.finishSession()

        cancelMediaUrlAssignment()
        atsc3Module.close()

        serviceGuideStore.clearAll()
        repository.reset()
    }

    override fun selectService(service: AVService) {
        // Reset current media. New media url will be received after service selection.
        cancelMediaUrlAssignment()
        repository.setMediaUrl(null)

        val res = atsc3Module.selectService(service.bsid, service.id)
        if (res) {
            atsc3Analytics.startSession(service.bsid, service.id, service.globalId, service.category)

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

            if (atsc3Module.slsProtocol == Atsc3Module.SLS_PROTOCOL_MMT) {
                repository.setMediaUrl("mmt://${service.id}")
            }
        } else {
            // Reset HELD and service if service can't be selected
            repository.setHeldPackage(null)
            repository.setSelectedService(null)
        }
    }

    override fun tune(frequency: PhyFrequency) {
        val freqKhz: Int = if (frequency.list.isEmpty()) {
            val lastFrequency = settings.lastFrequency
            val storedFrequency = settings.frequencyLocation?.firstFrequency
            if (lastFrequency > 0) {
                lastFrequency
            } else {
                storedFrequency ?: 0
            }
        } else {
            frequency.list.first()
        }

        this.freqKhz.postValue(freqKhz)
        settings.lastFrequency = freqKhz
        atsc3Module.tune(
                freqKhz = freqKhz,
                frequencies = frequency.list,
                retuneOnDemod = frequency.source == PhyFrequency.Source.USER
        )
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