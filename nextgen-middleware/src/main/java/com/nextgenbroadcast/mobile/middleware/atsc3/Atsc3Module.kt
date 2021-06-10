package com.nextgenbroadcast.mobile.middleware.atsc3

import android.util.Log
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.core.atsc3.MediaUrl
import com.nextgenbroadcast.mobile.core.atsc3.phy.PHYStatistics
import com.nextgenbroadcast.mobile.core.isEquals
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.Atsc3ServiceLocationTable
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.SLTConstants
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts.LLSParserAEAT
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3ApplicationFile
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3Held
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.HeldXmlParser
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.service.Atsc3Service
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.service.LLSParserSLT
import com.nextgenbroadcast.mobile.middleware.atsc3.source.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.ngbp.libatsc3.middleware.Atsc3NdkApplicationBridge
import org.ngbp.libatsc3.middleware.Atsc3NdkPHYBridge
import org.ngbp.libatsc3.middleware.android.a331.PackageExtractEnvelopeMetadataAndPayload
import org.ngbp.libatsc3.middleware.android.application.interfaces.IAtsc3NdkApplicationBridgeCallbacks
import org.ngbp.libatsc3.middleware.android.phy.interfaces.IAtsc3NdkPHYBridgeCallbacks
import org.ngbp.libatsc3.middleware.android.phy.models.BwPhyStatistics
import org.ngbp.libatsc3.middleware.android.phy.models.RfPhyStatistics
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.schedule
import kotlin.math.max

internal class Atsc3Module(
        private val cacheDir: File
) : IAtsc3Module, IAtsc3NdkApplicationBridgeCallbacks, IAtsc3NdkPHYBridgeCallbacks {

    private val atsc3NdkApplicationBridge = Atsc3NdkApplicationBridge(this)
    private val atsc3NdkPHYBridge = Atsc3NdkPHYBridge(this)

    private val stateLock = ReentrantLock()
    private var state = Atsc3ModuleState.IDLE
    private var source: IAtsc3Source? = null
    private var currentSourceConfiguration: Int = -1
    private var lastTunedFreqList: List<Int> = emptyList()
    private var defaultConfiguration: Map<Any, Atsc3ServiceLocationTable>? = null

    private val serviceLocationTable = ConcurrentHashMap<Int, Atsc3ServiceLocationTable>()
    private val serviceToSourceConfig = ConcurrentHashMap<Int, Int>()
    private val packageMap = HashMap<String, Atsc3Application>()

    var selectedServiceBsid = -1
        private set
    var selectedServiceId = -1
        private set
    private var suspendedServiceSelection: Boolean = false
    private var selectedServiceSLSProtocol = -1
    private var selectedServiceHeld: Atsc3Held? = null
    private var selectedServicePackage: Atsc3HeldPackage? = null
    private var selectedServiceHeldXml: String? = null //TODO: use TOI instead

    @Volatile
    private var phyDemodLock: Boolean = false

    @Volatile
    private var isReconfiguring: Boolean = false

    @Volatile
    private var listener: Atsc3ModuleListener? = null

    private var nextSourceJob: Job? = null

    private val configurationTimer = Timer()

    @Volatile
    private var nextSourceConfigTuneTimeoutTask: TimerTask? = null

    override val rfPhyMetricsFlow = MutableSharedFlow<RfPhyStatistics>(3, 0, BufferOverflow.DROP_OLDEST)

    override fun setListener(listener: Atsc3ModuleListener?) {
        if (this.listener != null) throw IllegalStateException("Atsc3Module listener already initialized")
        this.listener = listener
    }

    /**
     * Tune to one frequency or scan over all frequency in [frequencyList] and collect SLT from.
     * The first one will be used as default.
     * frequencyList - list of frequencies in KHz
     */
    override fun tune(frequencyList: List<Int>, force: Boolean) {
        val freqKhz = frequencyList.firstOrNull() ?: 0
        val demodLock = phyDemodLock
        if (!force && (demodLock || (freqKhz != 0 && lastTunedFreqList.isEquals(frequencyList)))) return

        val src = source
        if (src is ITunableSource) {
            reset()

            if (src is TunableConfigurableAtsc3Source) {
                src.setConfigs(frequencyList)

                if (USE_PERSISTED_CONFIGURATION) {
                    withStateLock {
                        applyDefaultConfiguration(src)
                    }
                }

                setSourceConfig(-1)
                val config = if (USE_PERSISTED_CONFIGURATION) {
                    findNextConfigIndexToSniff(src.getCurrentConfigIndex())
                } else {
                    src.getCurrentConfigIndex()
                }
                applySourceConfig(src, config, config > 0)

                lastTunedFreqList = frequencyList
            } else {
                lastTunedFreqList = listOf(freqKhz)

                src.tune(freqKhz)
            }
        }
    }

    override fun connect(source: IAtsc3Source, defaultConfig: Map<Any, Atsc3ServiceLocationTable>?): Boolean {
        log("Connecting to: $source")

        close()

        try {
            this.source = source

            return withStateLock {
                defaultConfiguration = defaultConfig
                if (USE_PERSISTED_CONFIGURATION) {
                    if (source.getConfigCount() > 0) {
                        applyDefaultConfiguration(source)

                        if (source is ConfigurableAtsc3Source<*>) {
                            val configIndex = findNextConfigIndexToSniff(source.getCurrentConfigIndex())
                            source.initCurrentConfiguration(configIndex)
                        }
                    }
                }

                val result = source.open()
                if (result == IAtsc3Source.RESULT_ERROR) {
                    close()
                } else {
                    setSourceConfig(result)
                    val newState = when {
                        result > 0 -> Atsc3ModuleState.SCANNING
                        source.getConfigCount() > 0 -> Atsc3ModuleState.SNIFFING
                        else -> Atsc3ModuleState.IDLE
                    }
                    setState(newState)
                    startSourceConfigTimeoutTask()
                    return@withStateLock true
                }
                return@withStateLock false
            }
        } catch (e: Exception) {
            LOG.e(TAG, "Error when opening the source: $source", e)

            this.source = null
        }

        return false
    }

    override fun cancelScanning() {
        cancelSourceConfigTimeoutTask()

        if (serviceLocationTable.isEmpty()) {
            setState(Atsc3ModuleState.IDLE)
        } else {
            finishReconfiguration()
        }
    }

    @Synchronized
    private fun applyNextSourceConfig() {
        val job = nextSourceJob
        log("applyNextSourceConfig with job: $job, isActive: ${job?.isActive}")

        if (job != null && job.isActive) {
            return
        }

        nextSourceJob = CoroutineScope(Dispatchers.Main).launch {
            // reset state before reconfiguration
            setSourceConfig(-1)
            val src = source
            if (src is ConfigurableAtsc3Source<*>) {
                val nextConfigIndex = if (USE_PERSISTED_CONFIGURATION) {
                    findNextConfigIndexToSniff(src.getCurrentConfigIndex() - 1)
                } else {
                    max(src.getCurrentConfigIndex() - 1, 0)
                }

                applySourceConfig(src, nextConfigIndex, true)
            }
        }
    }

    private fun <T> withStateLock(action: () -> T): T {
        try {
            stateLock.lock()

            return action()
        } finally {
            stateLock.unlock()
        }
    }

    private fun applySourceConfig(src: ConfigurableAtsc3Source<*>, config: Int, isScanning: Boolean = false): Int {
        cancelSourceConfigTimeoutTask()

        log("applySourceConfig with src: $src, config: $config")

        return withStateLock {
            if (src.getConfigCount() < 1) return@withStateLock IAtsc3Source.RESULT_ERROR

            isReconfiguring = true
            val result = try {
                src.configure(config)
            } finally {
                isReconfiguring = false
            }

            if (result != IAtsc3Source.RESULT_ERROR) {
                //jjustman-2021-05-19 - this is not quite true, we shouldn't actually set our "new" state until either onSls or onSourceConfigTimeout occurs..
                //vmatiash-2021-05-28 - refactored - we apply new configuration but to not switch to TUNED until receive SLT
                setSourceConfig(result)
                val newState = if (result > 0 && isScanning) Atsc3ModuleState.SCANNING else Atsc3ModuleState.SNIFFING
                setState(newState)
                startSourceConfigTimeoutTask()
            }

            return@withStateLock result
        }
    }

    @Synchronized
    private fun startSourceConfigTimeoutTask() {
        //failsafe if we don't acquire SLT
        // either wait on this block this coroutine, or the onSltTablePresent will invoke nextSourceConfigTuneTimeoutJob.cancel()
        LOG.i(TAG, "nextSourceConfigTuneTimeoutJob: tune SLT timeout - scheduled for $SLT_ACQUIRE_TUNE_DELAY ms")

        nextSourceConfigTuneTimeoutTask = configurationTimer.schedule(SLT_ACQUIRE_TUNE_DELAY) {
            // Remove SLT data for unreachable  configurations
            //TODO: probably we don't want it
            /*withStateLock {
                val currentConfig = getSourceConfig()
                serviceToSourceConfig.filterValues { it == currentConfig }.keys.forEach { bsid ->
                    serviceLocationTable.remove(bsid)
                    serviceToSourceConfig.remove(bsid)
                }
            }*/

            val currentState = getState()
            log("nextSourceConfigTuneTimeoutJob: tune SLT timeout - currentState: $currentState, invoking applyNextSourceConfig")
            if (currentState == Atsc3ModuleState.SCANNING) {
                applyNextSourceConfig()
            } else if (currentState != Atsc3ModuleState.IDLE) {
                if (serviceLocationTable.isEmpty()) {
                    // stop() - don't stop phy here to let it being reconfigured
                    setState(Atsc3ModuleState.IDLE)
                } else {
                    finishReconfiguration()
                }
            }
        }
    }

    private fun findNextConfigIndexToSniff(startConfigIndex: Int): Int {
        var nextConfigIndex = startConfigIndex
        while (nextConfigIndex >= 0 && serviceToSourceConfig.values.contains(nextConfigIndex)) {
            nextConfigIndex--
        }
        nextConfigIndex = max(nextConfigIndex, 0)
        return nextConfigIndex
    }

    @Synchronized
    private fun cancelSourceConfigTimeoutTask() {
        nextSourceConfigTuneTimeoutTask?.let {
            LOG.i(TAG, "nextSourceConfigTuneTimeoutJob: canceling")
            it.cancel()
            nextSourceConfigTuneTimeoutTask = null
        }
    }

    fun getState(): Atsc3ModuleState {
        return withStateLock {
            state
        }
    }

    private fun setState(newState: Atsc3ModuleState) {
        withStateLock {
            state = newState
        }
        listener?.onStateChanged(newState)
    }

    private fun getSourceConfig(): Int {
        return withStateLock {
            currentSourceConfiguration
        }
    }

    private fun setSourceConfig(config: Int) {
        withStateLock {
            currentSourceConfiguration = config
        }

        val configCount = when (val src = source) {
            null -> 0
            is ConfigurableAtsc3Source<*> -> src.getConfigCount()
            else -> 1
        }

        listener?.onConfigurationChanged(config, configCount, serviceToSourceConfig.values.contains(config))
    }

    override fun isServiceSelected(bsid: Int, serviceId: Int): Boolean {
        return selectedServiceBsid == bsid && selectedServiceId == serviceId
    }

    private var tmpAdditionalServiceOpened = false

    override fun selectService(bsid: Int, serviceId: Int): Boolean {
        if (selectedServiceBsid == bsid && (selectedServiceId == serviceId || state != Atsc3ModuleState.TUNED)) return false

        clearHeld()
        selectedServiceSLSProtocol = -1

        selectedServiceBsid = bsid
        selectedServiceId = serviceId

        serviceToSourceConfig[bsid]?.let { serviceConfig ->
            if (serviceConfig != getSourceConfig()) {
                val src = source
                if (src is ConfigurableAtsc3Source<*>) {
                    withStateLock {
                        applySourceConfig(src, serviceConfig)
                        suspendedServiceSelection = true
                    }
                    return true
                }
            }
        } ?: log("selectService - source configuration for bsid: $bsid NOT FOUND")

        return internalSelectService(bsid, serviceId)
    }

    private fun internalSelectService(bsid: Int, serviceId: Int): Boolean {
        log("internalSelectService: enter: with bsid: $bsid, serviceId: $serviceId");

        selectedServiceSLSProtocol = atsc3NdkApplicationBridge.atsc3_slt_selectService(serviceId)
        log("internalSelectService: after atsc3NdkApplicationBridge.atsc3_slt_selectService with serviceId: $serviceId, selectedServiceSLSProtocol is: $selectedServiceSLSProtocol")

        //TODO: temporary test solution
        if (!tmpAdditionalServiceOpened) {
            serviceLocationTable[bsid]?.services?.firstOrNull {
                it.serviceCategory == SLTConstants.SERVICE_CATEGORY_ESG
            }?.let { service ->
                log("internalSelectService, calling atsc3_slt_alc_select_additional_service with service.serviceId: $service.serviceId");

                tmpAdditionalServiceOpened = atsc3NdkApplicationBridge.atsc3_slt_alc_select_additional_service(service.serviceId) > 0
            }
        }

        if (selectedServiceSLSProtocol == SLS_PROTOCOL_MMT) {
            log("internalSelectService, calling listener?.onServiceMediaReady for MMT, listener: $listener, serviceId: $serviceId, bsid: $bsid")

            listener?.onServiceMediaReady(MediaUrl(SCHEME_MMT + serviceId, bsid, serviceId), 0)
        }

        return selectedServiceSLSProtocol > 0
    }

    @Deprecated("Do not work because additional service persistence not implemented in libatsc3")
    override fun selectAdditionalService(serviceId: Int): Boolean {
//        //atsc3NdkApplicationBridge.atsc3_slt_alc_clear_additional_service_selections()
//        val protocol = atsc3NdkApplicationBridge.atsc3_slt_alc_select_additional_service(serviceId)
//        return protocol > 0
        return false
    }

    override fun stop() {
        source?.stop()

        setState(Atsc3ModuleState.STOPPED)
    }

    override fun close() {
        source?.close()
        source = null

        defaultConfiguration = null

        lastTunedFreqList = emptyList()
        setSourceConfig(-1)
        reset()
    }

    override fun isIdle(): Boolean {
        return getState() == Atsc3ModuleState.IDLE
    }

    private fun reset() {
        setState(Atsc3ModuleState.IDLE)
        isReconfiguring = false
        suspendedServiceSelection = false
        clear()
        cancelSourceConfigTimeoutTask()
    }

    fun getSelectedServiceMediaUri(): String? {
        var mediaUri: String? = null
        if (selectedServiceSLSProtocol == SLS_PROTOCOL_DASH) {
            val routeMPDFileName = atsc3NdkApplicationBridge.atsc3_slt_alc_get_sls_metadata_fragments_content_locations_from_monitor_service_id(selectedServiceId, CONTENT_TYPE_DASH)
            if (routeMPDFileName.isNotEmpty()) {
                mediaUri = String.format("%s/%s", jni_getCacheDir(), routeMPDFileName[0])
            } else {
                log("Unable to resolve Dash MPD path from MBMS envelope, service_id: %d", selectedServiceId)
            }
        } else if (selectedServiceSLSProtocol == SLS_PROTOCOL_MMT) {
            mediaUri = SCHEME_MMT + selectedServiceId
        } else {
            log("unsupported service protocol: %d", selectedServiceSLSProtocol)
        }

        return mediaUri
    }

    private fun clear() {
        clearHeld()
        clearService()
        serviceLocationTable.clear()
        serviceToSourceConfig.clear()

        //TODO: temporary test solution
        if (tmpAdditionalServiceOpened) {
            atsc3NdkApplicationBridge.atsc3_slt_alc_clear_additional_service_selections()
            tmpAdditionalServiceOpened = false
        }
    }

    private fun clearService() {
        suspendedServiceSelection = false
        selectedServiceBsid = -1
        selectedServiceId = -1
        selectedServiceSLSProtocol = -1
    }

    private fun clearHeld() {
        selectedServiceHeld = null
        selectedServicePackage = null
        selectedServiceHeldXml = null

        /* it reconnects additional service on service selection that leads to ESG reloading - that's bad.
        But probably we need it when switching between services with different BSID. Lat's stose BSID in ESG data instead
        //TODO: temporary test solution
        if (tmpAdditionalServiceOpened) {
            atsc3NdkApplicationBridge.atsc3_slt_alc_clear_additional_service_selections()
            tmpAdditionalServiceOpened = false
        }*/
    }

    //////////////////////////////////////////////////////////////
    /// IAtsc3NdkApplicationBridgeCallbacks
    //////////////////////////////////////////////////////////////

    override fun showMsgFromNative(message: String) = log(message)

    override fun jni_getCacheDir(): File = cacheDir

    override fun onSltTablePresent(slt_payload_xml: String) {
        val shouldSkip = isReconfiguring

        cancelSourceConfigTimeoutTask()

        if (shouldSkip) {
            log("onSltTablePresent, currentState: uncertain, skip: $shouldSkip, slt_xml:\n$slt_payload_xml")
            return
        }

        val slt = LLSParserSLT().parseXML(slt_payload_xml)
        withStateLock {
            serviceLocationTable[slt.bsid] = slt
            serviceToSourceConfig[slt.bsid] = getSourceConfig()
        }

        // getState() is blocking method and must not be called if isReconfiguring = true
        val currentState = getState()

        log("onSltTablePresent, currentState: $currentState, skip: $shouldSkip, slt_xml:\n$slt_payload_xml")

        if (currentState == Atsc3ModuleState.SCANNING) {
            applyNextSourceConfig()
        } else if (currentState != Atsc3ModuleState.IDLE) {
            if (!suspendedServiceSelection) {
                selectedServiceBsid = slt.bsid
            }
            finishReconfiguration()
        }
    }

    private fun finishReconfiguration() {
        setState(Atsc3ModuleState.TUNED)

        processServiceLocationTableAndNotifyListener()

        if (suspendedServiceSelection) {
            CoroutineScope(Dispatchers.Main).launch {
                internalSelectService(selectedServiceBsid, selectedServiceId)
            }
        }
    }

    private fun processServiceLocationTableAndNotifyListener() {
        val services = serviceLocationTable
                .toSortedMap(compareBy { serviceToSourceConfig[it] })
                .values.flatMap { it.services }

        val urls = serviceLocationTable[selectedServiceBsid]?.urls ?: emptyMap()

        fireServiceLocationTableChanged(services, urls)
    }

    override fun onAeatTablePresent(aeatPayloadXML: String) {
        listener?.onAeatTableChanged(LLSParserAEAT().parseAeaTable(aeatPayloadXML))
    }

    override fun onSlsHeldEmissionPresent(serviceId: Int, heldPayloadXML: String) {
        if (getState() == Atsc3ModuleState.SCANNING) return

        log("onSlsHeldEmissionPresent, $serviceId, selectedServiceID: $selectedServiceId, HELD: $heldPayloadXML")

        if (serviceId == selectedServiceId) {
            if (heldPayloadXML != selectedServiceHeldXml) {
                selectedServiceHeldXml = heldPayloadXML

                val held = HeldXmlParser().parseXML(heldPayloadXML).also { held ->
                    selectedServiceHeld = held
                }

                if (held != null) {
                    val pkg = held.findActivePackage()
                    log("onSlsHeldEmissionPresent, pkg: $pkg")

                    if (pkg != selectedServicePackage) {
                        selectedServicePackage = pkg
                        listener?.onServicePackageChanged(pkg)
                    }
                }
            }
        }
    }

    override fun onAlcObjectStatusMessage(alc_object_status_message: String) {
        //TODO: notify value changed
    }

    override fun onAlcObjectClosed(service_id: Int, tsi: Int, toi: Int, s_tsid_content_location: String?, s_tsid_content_type: String?, cache_file_path: String?) {
        //if (getState() == State.SCANNING) return we do not open an additional service when scanning

        when (s_tsid_content_type) {
            CONTENT_TYPE_SGDD -> {
                // skip
            }

            CONTENT_TYPE_SGDU -> {
                //TODO: check that data from an actual source configuration or share current bsid/serviceId with SGDU
                cache_file_path?.let {
                    listener?.onServiceGuideUnitReceived(getFullPath(cache_file_path), selectedServiceBsid)
                }
            }
        }
    }

    override fun onPackageExtractCompleted(packageMetadata: PackageExtractEnvelopeMetadataAndPayload) {
        if (getState() == Atsc3ModuleState.SCANNING) return

        log("onPackageExtractCompleted packageExtractPath: ${packageMetadata.packageExtractPath} packageName: ${packageMetadata.packageName}, appContextIdList: ${packageMetadata.appContextIdList}, files: [${packageMetadata.multipartRelatedPayloadList.map { it.contentLocation }}]")

        if (!packageMetadata.isValid()) {
            log("onPackageExtractCompleted INVALID: $packageMetadata")
            return
        }

        val pkgUid = "${packageMetadata.packageExtractPath}/${packageMetadata.packageName}"

        val pkg = metadataToPackage(pkgUid, packageMetadata)

        val appPackage = packageMap[pkgUid]
        if (appPackage != pkg) {
            packageMap[pkgUid] = pkg
            listener?.onApplicationPackageReceived(pkg)
        }
    }

    override fun routeDash_force_player_reload_mpd(serviceID: Int) {
        LOG.i(TAG, "routeDash_force_player_reload_mpd with serviceId: $serviceID")
        if (getState() == Atsc3ModuleState.SCANNING) return

        if (serviceID == selectedServiceId) {
            getSelectedServiceMediaUri()?.let { mpdPath ->
                listener?.onServiceMediaReady(MediaUrl(mpdPath, selectedServiceBsid, serviceID), MPD_UPDATE_DELAY)
            }
        }
    }

    //////////////////////////////////////////////////////////////
    /// IAtsc3NdkPHYBridgeCallbacks
    //////////////////////////////////////////////////////////////

    override fun onPhyLogMessage(message: String) = log(message)

    override fun onPhyError(message: String) {
        listener?.onError("PHY Error: $message")
        log("PHY Error: $message")
    }

    override fun pushRfPhyStatisticsUpdate(rfPhyStatistics: RfPhyStatistics) {
        phyDemodLock = rfPhyStatistics.demod_lock != 0

        try {
            PHYStatistics.PHYRfStatistics = "PHY: $rfPhyStatistics".also {
                log(it)
            }
        } catch (ex: Exception) {
            Log.w(TAG, "exception when dumping PHYRFStatistics: $ex");
        }
        rfPhyMetricsFlow.tryEmit(rfPhyStatistics)
    }

    override fun pushBwPhyStatistics(bwPhyStatistics: BwPhyStatistics) {
        PHYStatistics.PHYBWStatistics = "BW: $bwPhyStatistics".also {
            log(it)
        }
    }

    //////////////////////////////////////////////////////////////

    override fun getCurrentConfiguration(): Pair<String, Map<Any, Atsc3ServiceLocationTable>>? {
        val src = source ?: return null
        return Pair(
                src::class.java.simpleName,
                serviceToSourceConfig.mapNotNull { (bsid, configIndex) ->
                    src.getConfigByIndex(configIndex).let { config ->
                        serviceLocationTable[bsid]?.let { slt ->
                            config to slt
                        }
                    }
                }.toMap()
        )
    }

    private fun applyDefaultConfiguration(src: IAtsc3Source) {
        val srcConfigs = src.getAllConfigs().map { it.toString() }
        defaultConfiguration?.forEach { (config, slt) ->
            val configIndex = srcConfigs.indexOf(config)
            if (configIndex >= 0) {
                serviceLocationTable[slt.bsid] = slt
                serviceToSourceConfig[slt.bsid] = configIndex
            }
        }
    }

    //jjustman-2021-05-19 - TODO: fix me to pass a proper collection of urls for <frequency, bsid, slt.groupId, SLTConstants.URL_TYPE_REPORT_SERVER> := { <XML> }
    private fun fireServiceLocationTableChanged(services: List<Atsc3Service>, urls: Map<Int, String>) {
        log("fireServiceLocationTableChanged, services: $services, urls: $urls")

        listener?.onServiceLocationTableChanged(
                selectedServiceBsid,
                Collections.unmodifiableList(services),
                urls[SLTConstants.URL_TYPE_REPORT_SERVER]
        )
    }

    private fun PackageExtractEnvelopeMetadataAndPayload.isValid(): Boolean {
        return !packageName.isNullOrEmpty()
                && !appContextIdList.isNullOrEmpty()
                && !packageExtractPath.isNullOrEmpty()
    }

    private fun metadataToPackage(uid: String, packageMetadata: PackageExtractEnvelopeMetadataAndPayload): Atsc3Application {
        val files = packageMetadata.multipartRelatedPayloadList?.map { file ->
            file.contentLocation to Atsc3ApplicationFile(file.contentLocation, file.contentType, file.version)
        }?.toMap() ?: emptyMap<String, Atsc3ApplicationFile>()

        return Atsc3Application(
                uid,
                packageMetadata.packageName,
                packageMetadata.appContextIdList.split(" "),
                getFullPath(packageMetadata.packageExtractPath),
                files
        )
    }

    private fun getFullPath(cachePath: String) = String.format("%s/%s", jni_getCacheDir(), cachePath)

    private fun log(text: String, vararg params: Any) {
        val msg = if (params.isNotEmpty()) {
            String.format(Locale.US, text, *params)
        } else {
            text
        }

        LOG.d(TAG, msg)
    }

    companion object {
        val TAG: String = Atsc3Module::class.java.simpleName

        private val SLT_ACQUIRE_TUNE_DELAY = TimeUnit.SECONDS.toMillis(20)

        private const val CONTENT_TYPE_DASH = "application/dash+xml"
        private const val CONTENT_TYPE_SGDD = "application/vnd.oma.bcast.sgdd+xml"
        private const val CONTENT_TYPE_SGDU = "application/vnd.oma.bcast.sgdu"

        private const val MPD_UPDATE_DELAY = 2000L

        const val RES_OK = 0

        const val SLS_PROTOCOL_DASH = 1
        const val SLS_PROTOCOL_MMT = 2

        const val SCHEME_MMT = "mmt://"

        private const val USE_PERSISTED_CONFIGURATION = true
    }
}