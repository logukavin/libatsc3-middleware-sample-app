package com.nextgenbroadcast.mobile.middleware.controller.service

import android.util.Log
import com.nextgenbroadcast.mobile.core.atsc3.MediaUrl
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.middleware.analytics.IAtsc3Analytics
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3ModuleListener
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3ModuleState
import com.nextgenbroadcast.mobile.middleware.atsc3.IAtsc3Module
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.SLTConstants
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts.AeaTable
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.service.Atsc3Service
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.IServiceGuideDeliveryUnitReader
import com.nextgenbroadcast.mobile.middleware.atsc3.source.*
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*


internal class ServiceControllerImpl (
        private val repository: IRepository,
        private val settings: IMiddlewareSettings,
        private val atsc3Module: IAtsc3Module,
        private val atsc3Analytics: IAtsc3Analytics,
        private val serviceGuideReader: IServiceGuideDeliveryUnitReader,
        private val stateScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        private val onError: ((message: String) -> Unit)? = null
) : IServiceController, Atsc3ModuleListener {
    private var heldResetJob: Job? = null
    private var mediaUrlAssignmentJob: Job? = null

    private val atsc3State = MutableStateFlow<Atsc3ModuleState?>(Atsc3ModuleState.IDLE)
    private val atsc3Configuration = MutableStateFlow<Pair<Int, Int>?>(null)

    override val selectedService = repository.selectedService
    override val serviceGuideUrls = repository.serviceGuideUrls
    override val applications = repository.applications

    override val receiverState: StateFlow<ReceiverState> = combine(atsc3State, repository.selectedService, atsc3Configuration) { state, service, config ->
        val (configIndex, configCount) = config ?: Pair(-1, -1)
        if (state == null || state == Atsc3ModuleState.IDLE) {
            ReceiverState.idle()
        } else if (state == Atsc3ModuleState.SCANNING) {
            ReceiverState.scanning(configIndex, configCount)
        } else if (service == null) {
            ReceiverState.tuning(configIndex, configCount)
        } else {
            ReceiverState.connected(configIndex, configCount)
        }
    }.stateIn(stateScope, SharingStarted.Eagerly, ReceiverState.idle())

    override val receiverFrequency = MutableStateFlow(0)

    override val routeServices: StateFlow<List<AVService>> = repository.services.map { services ->
        services.filter { !it.hidden }
    }.stateIn(stateScope, SharingStarted.Eagerly, emptyList())

    override val alertList = repository.alertsForNotify

    init {
        atsc3Module.setListener(this)
    }

    override fun onStateChanged(state: Atsc3ModuleState) {
        atsc3State.value = state
        if (state == Atsc3ModuleState.IDLE) {
            repository.reset()
        }
    }

    override fun onConfigurationChanged(index: Int, count: Int) {
        atsc3Configuration.value = Pair(index, count)
    }

    override fun onApplicationPackageReceived(appPackage: Atsc3Application) {
        Log.d("ServiceControllerImpl", "onPackageReceived - appPackage: $appPackage")

        repository.addOrUpdateApplication(appPackage)
    }

    override fun onServiceLocationTableChanged(services: List<Atsc3Service>, reportServerUrl: String?) {
        atsc3Analytics.setReportServerUrl(reportServerUrl)

        // store A/V services
        val avServices = services.filter { service ->
            service.serviceCategory == SLTConstants.SERVICE_CATEGORY_AV
                    || service.serviceCategory == SLTConstants.SERVICE_CATEGORY_AO
                    || service.serviceCategory == SLTConstants.SERVICE_CATEGORY_ABS
        }.map {
            AVService(
                    it.bsid,
                    it.serviceId,
                    it.shortServiceName,
                    it.globalServiceId,
                    it.majorChannelNo,
                    it.minorChannelNo,
                    it.serviceCategory,
                    it.hidden
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

    override fun onServicePackageChanged(pkg: Atsc3HeldPackage?) {
        cancelHeldReset()

        repository.setHeldPackage(pkg)
    }

    override fun onServiceMediaReady(mediaUrl: MediaUrl, delayBeforePlayMs: Long) {
        if (delayBeforePlayMs > 0) {
            setMediaUrlWithDelay(mediaUrl, delayBeforePlayMs)
        } else {
            repository.setMediaUrl(mediaUrl)
        }
    }

    override fun onServiceGuideUnitReceived(filePath: String, bsid: Int) {
        serviceGuideReader.readDeliveryUnit(filePath, bsid)
    }

    override fun onError(message: String) {
        onError?.invoke(message)
    }

    override fun onAeatTableChanged(list: List<AeaTable>) {
        repository.setAlertList(list)
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

        serviceGuideReader.clearAll()
        repository.reset()
    }

    override fun selectService(service: AVService): Boolean {
        if (repository.selectedService.value == service) return true

        // Reset current media. New media url will be received after service selection.
        cancelMediaUrlAssignment()
        repository.setMediaUrl(null)

        if (atsc3Module.isServiceSelected(service.bsid, service.id)) return true

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
        } else {
            // Reset HELD and service if service can't be selected
            repository.setHeldPackage(null)
            repository.setSelectedService(null)
        }

        return res
    }

    override fun tune(frequency: PhyFrequency) {
        val frequencyList: List<Int> = if (frequency.list.isEmpty()) {
            val lastFrequency = settings.lastFrequency
            settings.frequencyLocation?.let {
                it.frequencyList.toMutableList().apply {
                    if (lastFrequency > 0) {
                        remove(lastFrequency)
                        add(0, lastFrequency)
                    }
                }
            } ?: mutableListOf<Int>().apply {
                if (lastFrequency > 0) {
                    add(lastFrequency)
                }
            }
        } else {
            frequency.list
        }

        val freqKhz = frequencyList.firstOrNull() ?: return

        receiverFrequency.value = freqKhz
        settings.lastFrequency = freqKhz
        atsc3Module.tune(
                freqKhz = freqKhz,
                frequencies = frequencyList,
                retuneOnDemod = frequency.source == PhyFrequency.Source.USER
        )
    }

    override fun findServiceById(globalServiceId: String): AVService? {
        return repository.findServiceBy(globalServiceId)
    }

    override fun getNearbyService(offset: Int): AVService? {
        return repository.selectedService.value?.let { activeService ->
            routeServices.value.let { services ->
                val activeServiceIndex = services.indexOf(activeService)
                services.getOrNull(activeServiceIndex + offset)
            }
        }
    }

    override fun getCurrentService(): AVService? {
        return repository.selectedService.value
    }

    override fun getCurrentRouteMediaUrl(): MediaUrl? {
        return repository.routeMediaUrl.value
    }

    private fun resetHeldWithDelay() {
        cancelHeldReset()
        heldResetJob = stateScope.launch {
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

    private fun setMediaUrlWithDelay(mediaUrl: MediaUrl, delayMs: Long) {
        cancelMediaUrlAssignment()
        mediaUrlAssignmentJob = stateScope.launch {
            delay(delayMs)
            withContext(Dispatchers.Main) {
                repository.setMediaUrl(mediaUrl)
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
    }
}