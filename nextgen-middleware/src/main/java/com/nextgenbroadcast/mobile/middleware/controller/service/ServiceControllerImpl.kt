package com.nextgenbroadcast.mobile.middleware.controller.service

import android.location.Location
import android.util.Log
import com.nextgenbroadcast.mobile.core.atsc3.MediaUrl
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.middleware.analytics.IAtsc3Analytics
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3ModuleListener
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3ModuleState
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3Profile
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


internal class ServiceControllerImpl(
        private val repository: IRepository,
        private val settings: IMiddlewareSettings,
        private val atsc3Module: IAtsc3Module,
        private val atsc3Analytics: IAtsc3Analytics,
        private val serviceGuideReader: IServiceGuideDeliveryUnitReader,
        private val stateScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        private val onError: ((message: String) -> Unit)? = null
) : IServiceController, Atsc3ModuleListener {

    private val atsc3Scope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    private val atsc3ScopeLock = Mutex()

    private var heldResetJob: Job? = null
    private var mediaUrlAssignmentJob: Job? = null

    private val atsc3State = MutableStateFlow<Atsc3ModuleState?>(Atsc3ModuleState.IDLE)
    private val atsc3Configuration = MutableStateFlow<Triple<Int, Int, Boolean>?>(null)

    override val selectedService = repository.selectedService
    override val serviceGuideUrls = repository.serviceGuideUrls
    override val applications = repository.applications

    override val receiverState: StateFlow<ReceiverState> = combine(atsc3State, repository.selectedService, atsc3Configuration) { state, service, config ->
        val (configIndex, configCount, configKnown) = config ?: Triple(-1, -1, false)
        if (state == null || state == Atsc3ModuleState.IDLE) {
            ReceiverState.idle()
        } else if (state == Atsc3ModuleState.SCANNING || (state == Atsc3ModuleState.SNIFFING && !configKnown && configCount > 1)) {
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
        if (state == Atsc3ModuleState.IDLE) {
            repository.reset()
        }
        atsc3State.value = state
    }

    override fun onConfigurationChanged(index: Int, count: Int, isKnown: Boolean) {
        atsc3Configuration.value = Triple(index, count, isKnown)
    }

    override fun onApplicationPackageReceived(appPackage: Atsc3Application) {
        Log.d("ServiceControllerImpl", "onPackageReceived - appPackage: $appPackage")

        repository.addOrUpdateApplication(appPackage)
    }

    override fun onServiceLocationTableChanged(bsid: Int, services: List<Atsc3Service>, reportServerUrl: String?) {
        atsc3Analytics.setReportServerUrl(bsid, reportServerUrl)

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

        val oldServices = repository.services.value
        if (oldServices.size != avServices.size || !oldServices.containsAll(avServices)) {
            storeCurrentProfile()
        }

        repository.setServices(avServices)

        // select ESG service
        services.firstOrNull { service ->
            service.serviceCategory == SLTConstants.SERVICE_CATEGORY_ESG
        }?.let { service ->
            atsc3Scope.launch {
                atsc3Module.selectAdditionalService(service.serviceId)
            }
        }
    }

    override fun onServicePackageChanged(pkg: Atsc3HeldPackage?) {
        cancelHeldReset()

        repository.setHeldPackage(pkg)
    }

    override fun onServiceMediaReady(mediaUrl: MediaUrl, delayBeforePlayMs: Long) {
        Log.e(TAG, String.format("onServiceMediaReady with mediaUrl: %s, delayBeforePlayMs: %d", mediaUrl, delayBeforePlayMs))
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

    override suspend fun openRoute(source: IAtsc3Source, force: Boolean): Boolean {
        return withContext(atsc3Scope.coroutineContext) {
            atsc3ScopeLock.withLock {
                if (force || atsc3Module.isIdle()) {
                    withContext(Dispatchers.Main) {
                        clearRouteData()
                    }

                    val lastProfile = getLastProfileForSource(source)
                    if (atsc3Module.connect(source, lastProfile?.configs)) {
                        if (source is ITunableSource) {
                            tune(PhyFrequency.default(PhyFrequency.Source.AUTO))
                        }
                        return@withLock true
                    }
                }
                return@withLock false
            }
        }
    }

    override suspend fun closeRoute() {
        withContext(atsc3Scope.coroutineContext) {
            atsc3Module.stop() // call to stopRoute is not a mistake. We use it to close previously opened file
            atsc3Module.close()
        }

        withContext(Dispatchers.Main) {
            clearRouteData()
        }
    }

    private fun clearRouteData() {
        atsc3Analytics.finishSession()

        cancelMediaUrlAssignment()

        serviceGuideReader.clearAll()
        repository.reset()
    }

    override suspend fun selectService(service: AVService): Boolean {
        return withContext(Dispatchers.Main) {
            if (repository.selectedService.value == service) return@withContext true

            // Reset current media. New media url will be received after service selection.
            resetMediaUrl()

            val res = withContext(atsc3Scope.coroutineContext) {
                if (!atsc3Module.isServiceSelected(service.bsid, service.id)) {
                    atsc3Module.selectService(service.bsid, service.id)
                } else null
            } ?: return@withContext true

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

            res
        }
    }

    override suspend fun cancelScanning() {
        withContext(atsc3Scope.coroutineContext) {
            atsc3Module.cancelScanning()
        }
    }

    override suspend fun tune(frequency: PhyFrequency) {
        withContext(Dispatchers.Main) {
            val frequencyList: List<Int> = if (frequency.list.isEmpty()) {
                // If provided frequency list is empty then load last saved frequency and frequency list if it available
                val lastFrequency = settings.lastFrequency
                //TODO: why don't we check location??
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

            val freqKhz = frequencyList.firstOrNull() ?: return@withContext

            val forceTune = (frequency.source == PhyFrequency.Source.USER)

            if (forceTune) {
                // Reset current media. New media url will be received after service selection.
                resetMediaUrl()
            }

            // Store the first one because it will be used as default
            receiverFrequency.value = freqKhz
            settings.lastFrequency = freqKhz

            withContext(atsc3Scope.coroutineContext) {
                // ignore auto tune if receiver already tuned or scanning
                if (atsc3Module.isIdle() || forceTune) {
                    atsc3Module.tune(frequencyList, forceTune)
                }
            }
        }
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

    private fun resetMediaUrl() {
        cancelMediaUrlAssignment()
        repository.setMediaUrl(null)
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

    private fun getLastProfileForSource(source: IAtsc3Source): Atsc3Profile? {
        val profile = settings.receiverProfile
        return profile?.let {
            val elapsedTime = System.currentTimeMillis() - profile.timestamp
            val deviceLocation = repository.lastLocation.value
            val profileLocation = Location("unknown").apply {
                latitude = profile.location.lat
                longitude = profile.location.lng
            }
            if (source::class.java.simpleName == profile.sourceType
                    && deviceLocation != null
                    && deviceLocation.distanceTo(profileLocation) < PROFILE_LOCATION_RADIUS
                    && elapsedTime > 0 && elapsedTime < PROFILE_LIFE_TIME) {
                profile
            } else {
                null
            }
        }
    }

    private fun storeCurrentProfile() {
        atsc3Scope.launch {
            val location = repository.lastLocation.value
            if (location != null && location.latitude != 0.0 && location.longitude != 0.0) {
                settings.receiverProfile = atsc3Module.getCurrentConfiguration()?.let { (srcType, config) ->
                    Atsc3Profile(
                            srcType,
                            config,
                            System.currentTimeMillis(),
                            Atsc3Profile.SimpleLocation(location.latitude, location.longitude)
                    )
                }
            }
        }
    }

    companion object {
        val TAG: String = ServiceControllerImpl::class.java.simpleName

        private val BA_LOADING_TIMEOUT = TimeUnit.SECONDS.toMillis(5)
        private val PROFILE_LIFE_TIME = TimeUnit.DAYS.toMillis(30)

        private const val PROFILE_LOCATION_RADIUS = 24 * 1000 // about 15 mile
    }
}