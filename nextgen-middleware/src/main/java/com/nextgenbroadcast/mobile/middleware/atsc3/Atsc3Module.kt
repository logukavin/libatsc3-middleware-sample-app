package com.nextgenbroadcast.mobile.middleware.atsc3

import android.content.Context
import android.util.Log
import android.util.SparseArray
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
import org.ngbp.libatsc3.middleware.android.phy.Atsc3NdkPHYClientBase
import org.ngbp.libatsc3.middleware.android.phy.Atsc3UsbDevice
import org.ngbp.libatsc3.middleware.android.application.interfaces.IAtsc3NdkMediaMMTBridgeCallbacks
import org.ngbp.libatsc3.middleware.android.mmt.MfuByteBufferFragment
import org.ngbp.libatsc3.middleware.android.mmt.MpuMetadata_HEVC_NAL_Payload
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
        fun onServiceLocationTableChanged(services: List<Atsc3Service>, reportServerUrl: String?)
        fun onPackageReceived(appPackage: Atsc3Application)
        fun onCurrentServicePackageChanged(pkg: Atsc3HeldPackage?)
        fun onCurrentServiceDashPatched(mpdPath: String)
        fun onServiceGuideUnitReceived(filePath: String)
    }

    private val atsc3NdkApplicationBridge = Atsc3NdkApplicationBridge(this)
    private val atsc3NdkPHYBridge = Atsc3NdkPHYBridge(this)

    private val stateLock = ReentrantLock()
    private var state = State.IDLE
    private var source: IAtsc3Source? = null
    private var currentSourceConfiguration: Int = IAtsc3Source.CONFIG_DEFAULT
    private var lastTunedFreqKhz: Int = 0

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

    val slsProtocol: Int
        get() = selectedServiceSLSProtocol

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
        val demodLock = phyDemodLock
        if ((freqKhz != 0 && lastTunedFreqKhz == freqKhz) || (!retuneOnDemod && demodLock)) return

        val src = source
        if (src is ITunableSource) {
            reset()

            if (src is TunableConfigurableAtsc3Source) {
                src.setConfigs(
                        frequencies.toMutableList().apply {
                            // make target frequency first
                            remove(freqKhz)
                            add(0, freqKhz)
                        }
                )

                setSourceConfig(IAtsc3Source.CONFIG_DEFAULT)
                applySourceConfig(src, -1)
            } else {
                lastTunedFreqKhz = freqKhz

                src.tune(freqKhz)
            }
        }
    }

    fun connect(source: IAtsc3Source): Boolean {
        log("Connecting to: $source")

        close()

        this.source = source

        try {
            stateLock.lock()

            val result = source.open()
            if (result != IAtsc3Source.RESULT_ERROR) {
                setSourceConfig(result)
                setState(
                        if (result > 0) State.SCANNING else State.OPENED
                )
                return true
            }
        } finally {
            stateLock.unlock()
        }

        return false
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

    private fun applySourceConfig(src: ConfigurableAtsc3Source<*>, config: Int): Int {
        val result = src.configure(config)
        if (result != IAtsc3Source.RESULT_ERROR) {
            setSourceConfig(result)
            setState(
                    if (result > 0 && config == IAtsc3Source.CONFIG_DEFAULT) State.SCANNING else State.OPENED
            )
        }
        return result
    }

    private fun getState(): State {
        try {
            stateLock.lock()
            return state
        } finally {
            stateLock.unlock()
        }
    }

    private fun setState(newState: State) {
        try {
            stateLock.lock()
            state = newState
        } finally {
            stateLock.unlock()
        }
        listener?.onStateChanged(newState)
    }

    private fun getSourceConfig(): Int {
        try {
            stateLock.lock()
            return currentSourceConfiguration
        } finally {
            stateLock.unlock()
        }
    }

    private fun setSourceConfig(config: Int) {
        try {
            stateLock.lock()
            currentSourceConfiguration = config
        } finally {
            stateLock.unlock()
        }
    }

    private var tmpAdditionalServiceOpened = false

    fun selectService(bsid: Int, serviceId: Int): Boolean {
        if (selectedServiceBsid == bsid && selectedServiceId == serviceId) return false

        clearHeld()

        selectedServiceBsid = bsid
        selectedServiceId = serviceId

        serviceToSourceConfig[bsid]?.let { serviceConfig ->
            if (serviceConfig != getSourceConfig()) {
                val src = source
                if (src is ConfigurableAtsc3Source<*>) {
                    applySourceConfig(src, serviceConfig)

                    suspendedServiceSelection = true
                    return true
                }
            }
        }

        return internalSelectService(bsid, serviceId)
    }

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

        lastTunedFreqKhz = 0
        setSourceConfig(IAtsc3Source.CONFIG_DEFAULT)
        reset()
    }

    private fun reset() {
        setState(State.IDLE)
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
        log("onSlsTablePresent, $sls_payload_xml")

        val slt = LLSParserSLT().parseXML(sls_payload_xml)
        serviceLocationTable[slt.bsid] = slt
        serviceToSourceConfig[slt.bsid] = getSourceConfig()

        Log.d("!!!", "serviceLocationTable: $serviceLocationTable")

        if (getState() != State.SCANNING) {
            val services = serviceLocationTable.values.flatMap { it.services }
            fireServiceLocationTableChanged(services, slt.urls)

            if (suspendedServiceSelection) {
                internalSelectService(selectedServiceBsid, selectedServiceId)
            }
        } else {
            applyNextSourceConfig()
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
                        listener?.onCurrentServicePackageChanged(pkg)
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
                if (selectedServiceId == service_id) {
                    cache_file_path?.let {
                        listener?.onServiceGuideUnitReceived(getFullPath(cache_file_path))
                    }
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
            listener?.onPackageReceived(pkg)
        }
    }

    override fun routeDash_force_player_reload_mpd(ServiceID: Int) {
        if (getState() == State.SCANNING) return

        if (ServiceID == selectedServiceId) {
            getSelectedServiceMediaUri()?.let { mpdPath ->
                listener?.onCurrentServiceDashPatched(mpdPath)
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

        const val RES_OK = 0

        const val SLS_PROTOCOL_DASH = 1
        const val SLS_PROTOCOL_MMT = 2

    }
}