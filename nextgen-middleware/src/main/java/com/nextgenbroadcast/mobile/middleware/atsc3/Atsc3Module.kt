package com.nextgenbroadcast.mobile.middleware.atsc3

import android.content.Context
import android.util.Log
import android.util.SparseArray
import androidx.annotation.MainThread
import com.nextgenbroadcast.mobile.core.isEquals
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.Atsc3ServiceLocationTable
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.SLTConstants
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3ApplicationFile
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3Held
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.HeldXmlParser
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.service.Atsc3Service
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.service.LLSParserSLT
import com.nextgenbroadcast.mobile.middleware.phy.Atsc3DeviceReceiver
import com.nextgenbroadcast.mobile.middleware.atsc3.source.ConfigurableAtsc3Source
import com.nextgenbroadcast.mobile.middleware.atsc3.source.IAtsc3Source
import com.nextgenbroadcast.mobile.middleware.atsc3.source.ITunableSource
import com.nextgenbroadcast.mobile.middleware.atsc3.source.TunableConfigurableAtsc3Source
import kotlinx.coroutines.*
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
import java.util.concurrent.locks.ReentrantLock

internal class Atsc3Module(
        private val context: Context
): IAtsc3NdkApplicationBridgeCallbacks, IAtsc3NdkPHYBridgeCallbacks {

    enum class State {
        SCANNING, OPENED, PAUSED, IDLE
    }

    interface Listener {
        fun onStateChanged(state: State)
        fun onApplicationPackageReceived(appPackage: Atsc3Application)
        fun onServiceLocationTableChanged(services: List<Atsc3Service>, reportServerUrl: String?)
        fun onServicePackageChanged(pkg: Atsc3HeldPackage?)
        fun onServiceMediaReady(path: String, delayBeforePlayMs: Long)
        fun onServiceGuideUnitReceived(filePath: String)
    }

    private val atsc3NdkApplicationBridge = Atsc3NdkApplicationBridge(this)
    private val atsc3NdkPHYBridge = Atsc3NdkPHYBridge(this)

    private val stateLock = ReentrantLock()
    private var state = State.IDLE
    private var source: IAtsc3Source? = null
    private var currentSourceConfiguration: Int = IAtsc3Source.CONFIG_DEFAULT
    private var lastTunedFreqList: List<Int> = emptyList()

    private val serviceLocationTable = ConcurrentHashMap<Int, Atsc3ServiceLocationTable>()
    private val serviceToSourceConfig = ConcurrentHashMap<Int, Int>()
    private val packageMap = HashMap<String, Atsc3Application>()

    private var selectedServiceBsid = -1
    private var selectedServiceId = -1
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
    private var listener: Listener? = null

    private var nextSourceJob: Job? = null

    fun setListener(listener: Listener?) {
        if (this.listener != null) throw IllegalStateException("Atsc3Module listener already initialized")
        this.listener = listener
    }

    /**
     * Tune to [freqKhz] is it's greater thar zero. The [frequencies] allows to scan and collect data from all of them.
     */
    fun tune(freqKhz: Int, frequencies: List<Int>, retuneOnDemod: Boolean) {
        // make target frequency first
        val frequencyList = frequencies.toMutableList().apply {
            remove(freqKhz)
            add(0, freqKhz)
        }
        val demodLock = phyDemodLock
        if ((freqKhz != 0 && lastTunedFreqList.isEquals(frequencyList)) || (!retuneOnDemod && demodLock)) return

        val src = source
        if (src is ITunableSource) {
            reset()

            if (src is TunableConfigurableAtsc3Source) {
                src.setConfigs(frequencyList)

                setSourceConfig(IAtsc3Source.CONFIG_DEFAULT)
                applySourceConfig(src, -1)

                lastTunedFreqList = frequencyList
            } else {
                lastTunedFreqList = listOf(freqKhz)

                src.tune(freqKhz)
            }
        }
    }

    fun connect(source: IAtsc3Source): Boolean {
        log("Connecting to: $source")

        close()

        this.source = source

        return withStateLock {
            val result = source.open()
            if (result != IAtsc3Source.RESULT_ERROR) {
                setSourceConfig(result)
                setState(
                        if (result > 0) State.SCANNING else State.OPENED
                )
                return@withStateLock true
            }
            return@withStateLock false
        }
    }

    @Synchronized
    private fun applyNextSourceConfig() {
        val job = nextSourceJob
        if (job != null && job.isActive) {
            return
        }

        nextSourceJob = CoroutineScope(Dispatchers.Main).launch {
            setSourceConfig(IAtsc3Source.CONFIG_DEFAULT)
            val src = source
            if (src is ConfigurableAtsc3Source<*>) {
                applySourceConfig(src, -1 /* apply next config */)
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

    private fun applySourceConfig(src: ConfigurableAtsc3Source<*>, config: Int): Int {
        return withStateLock {
            isReconfiguring = true
            val result = try {
                src.configure(config)
            } finally {
                isReconfiguring = false
            }

            if (result != IAtsc3Source.RESULT_ERROR) {
                setSourceConfig(result)
                setState(
                        if (result > 0 && config == IAtsc3Source.CONFIG_DEFAULT) State.SCANNING else State.OPENED
                )
            }

            return@withStateLock result
        }
    }

    private fun getState(): State {
        return withStateLock {
            state
        }
    }

    private fun setState(newState: State) {
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
    }

    private var tmpAdditionalServiceOpened = false

    fun selectService(bsid: Int, serviceId: Int): Boolean {
        if (selectedServiceBsid == bsid && selectedServiceId == serviceId) return false

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
        }

        return internalSelectService(bsid, serviceId)
    }

    @MainThread
    private fun internalSelectService(bsid: Int, serviceId: Int): Boolean {
        selectedServiceSLSProtocol = atsc3NdkApplicationBridge.atsc3_slt_selectService(serviceId)

        //TODO: temporary test solution
        if (!tmpAdditionalServiceOpened) {
            serviceLocationTable[bsid]?.services?.firstOrNull {
                it.serviceCategory == SLTConstants.SERVICE_CATEGORY_ESG
            }?.let { service ->
                tmpAdditionalServiceOpened = atsc3NdkApplicationBridge.atsc3_slt_alc_select_additional_service(service.serviceId) > 0
            }
        }

        if (selectedServiceSLSProtocol == SLS_PROTOCOL_MMT) {
            listener?.onServiceMediaReady(SCHEME_MMT + serviceId, 0)
        }

        return selectedServiceSLSProtocol > 0
    }

    @Deprecated("Do not work because additional service persistence not implemented in libatsc3")
    fun selectAdditionalService(serviceId: Int): Boolean {
//        //atsc3NdkApplicationBridge.atsc3_slt_alc_clear_additional_service_selections()
//        val protocol = atsc3NdkApplicationBridge.atsc3_slt_alc_select_additional_service(serviceId)
//        return protocol > 0
        return false
    }

    fun stop() {
        source?.stop()

        setState(State.PAUSED)
    }

    fun close() {
        source?.close()

        lastTunedFreqList = emptyList()
        setSourceConfig(IAtsc3Source.CONFIG_DEFAULT)
        reset()
    }

    private fun reset() {
        setState(State.IDLE)
        isReconfiguring = false
        clear()
    }

    private fun getSelectedServiceMediaUri(): String? {
        var mediaUri: String? = null
        if (selectedServiceSLSProtocol == SLS_PROTOCOL_DASH) {
            val routeMPDFileName = atsc3NdkApplicationBridge.atsc3_slt_alc_get_sls_metadata_fragments_content_locations_from_monitor_service_id(selectedServiceId, CONTENT_TYPE_DASH)
            if (routeMPDFileName.isNotEmpty()) {
                mediaUri = String.format("%s/%s", jni_getCacheDir(), routeMPDFileName[0])
            } else {
                log("Unable to resolve Dash MPD path from MBMS envelope, service_id: %d", selectedServiceId)
            }
        } /*else if (selectedServiceSLSProtocol == 2) {
            //TODO: add support
        }*/ else {
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

        //TODO: temporary test solution
        if (tmpAdditionalServiceOpened) {
            atsc3NdkApplicationBridge.atsc3_slt_alc_clear_additional_service_selections()
            tmpAdditionalServiceOpened = false
        }
    }

    //////////////////////////////////////////////////////////////
    /// IAtsc3NdkApplicationBridgeCallbacks
    //////////////////////////////////////////////////////////////

    override fun showMsgFromNative(message: String) = log(message)

    override fun jni_getCacheDir(): File = context.cacheDir

    override fun onSlsTablePresent(sls_payload_xml: String) {
        val shouldSkip = isReconfiguring

        log("onSlsTablePresent, $sls_payload_xml, skip: $shouldSkip")

        if (shouldSkip) return

        val slt = LLSParserSLT().parseXML(sls_payload_xml)
        serviceLocationTable[slt.bsid] = slt
        serviceToSourceConfig[slt.bsid] = getSourceConfig()

        val currentState = getState()
        if (currentState == State.SCANNING) {
            applyNextSourceConfig()
        } else if (currentState != State.IDLE) {
            val services = serviceLocationTable
                    .toSortedMap(compareBy { serviceToSourceConfig[it] })
                    .values.flatMap { it.services }
            fireServiceLocationTableChanged(services, slt.urls)

            if (suspendedServiceSelection) {
                CoroutineScope(Dispatchers.Main).launch {
                    internalSelectService(selectedServiceBsid, selectedServiceId)
                }
            }
        }
    }

    override fun onAeatTablePresent(aeatPayloadXML: String) {
        //TODO("Not yet implemented")
    }

    override fun onSlsHeldEmissionPresent(serviceId: Int, heldPayloadXML: String) {
        if (getState() == State.SCANNING) return

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
                    listener?.onServiceGuideUnitReceived(getFullPath(cache_file_path))
                }
            }
        }
    }

    override fun onPackageExtractCompleted(packageMetadata: PackageExtractEnvelopeMetadataAndPayload) {
        if (getState() == State.SCANNING) return

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

    override fun routeDash_force_player_reload_mpd(ServiceID: Int) {
        if (getState() == State.SCANNING) return

        if (ServiceID == selectedServiceId) {
            getSelectedServiceMediaUri()?.let { mpdPath ->
                listener?.onServiceMediaReady(mpdPath, MPD_UPDATE_DELAY)
            }
        }
    }

    //////////////////////////////////////////////////////////////
    /// IAtsc3NdkPHYBridgeCallbacks
    //////////////////////////////////////////////////////////////

    override fun pushRfPhyStatisticsUpdate(rfPhyStatistics: RfPhyStatistics) {
        phyDemodLock = rfPhyStatistics.demod_lock != 0
        Log.i("Atsc3Module",String.format("PHY:RFStatisticsUpdate: %s", rfPhyStatistics.toString()))
        Atsc3DeviceReceiver.PHYRfStatistics = String.format("PHY:RFStatisticsUpdate: %s", rfPhyStatistics.toString())
    }

    override fun pushBwPhyStatistics(bwPhyStatistics: BwPhyStatistics) {
        Log.i("Atsc3Module",String.format("PHY:BWStatisticsUpdate: %s", bwPhyStatistics.toString()));
        Atsc3DeviceReceiver.PHYBWStatistics = String.format("PHY:BWStatisticsUpdate: %s", bwPhyStatistics.toString())
    }

    //////////////////////////////////////////////////////////////

    private fun fireServiceLocationTableChanged(services: List<Atsc3Service>, urls: SparseArray<String>) {
        listener?.onServiceLocationTableChanged(
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

        Log.d(TAG, msg)
    }

    companion object {
        val TAG: String = Atsc3Module::class.java.simpleName

        private const val CONTENT_TYPE_DASH = "application/dash+xml"
        private const val CONTENT_TYPE_SGDD = "application/vnd.oma.bcast.sgdd+xml"
        private const val CONTENT_TYPE_SGDU = "application/vnd.oma.bcast.sgdu"

        private const val MPD_UPDATE_DELAY = 2000L

        const val RES_OK = 0

        const val SLS_PROTOCOL_DASH = 1
        const val SLS_PROTOCOL_MMT = 2

        const val SCHEME_MMT = "mmt://"
    }
}