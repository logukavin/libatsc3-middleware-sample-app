package com.nextgenbroadcast.mobile.middleware.atsc3

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.nextgenbroadcast.mobile.core.media.IMMTDataConsumer
import com.nextgenbroadcast.mobile.core.media.IMMTDataProducer
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.SLTConstants
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3ApplicationFile
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3Held
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.HeldXmlParser
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.service.Atsc3Service
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.service.LLSParserSLT
import org.ngbp.libatsc3.middleware.Atsc3NdkApplicationBridge
import org.ngbp.libatsc3.middleware.Atsc3NdkMediaMMTBridge
import org.ngbp.libatsc3.middleware.Atsc3NdkPHYBridge
import org.ngbp.libatsc3.middleware.android.ATSC3PlayerFlags
import org.ngbp.libatsc3.middleware.android.a331.PackageExtractEnvelopeMetadataAndPayload
import org.ngbp.libatsc3.middleware.android.application.interfaces.IAtsc3NdkApplicationBridgeCallbacks
import org.ngbp.libatsc3.middleware.android.application.interfaces.IAtsc3NdkMediaMMTBridgeCallbacks
import org.ngbp.libatsc3.middleware.android.mmt.MfuByteBufferFragment
import org.ngbp.libatsc3.middleware.android.mmt.MpuMetadata_HEVC_NAL_Payload
import org.ngbp.libatsc3.middleware.android.mmt.models.MMTAudioDecoderConfigurationRecord
import org.ngbp.libatsc3.middleware.android.phy.Atsc3NdkPHYClientBase
import org.ngbp.libatsc3.middleware.android.phy.Atsc3UsbDevice
import org.ngbp.libatsc3.middleware.android.phy.interfaces.IAtsc3NdkPHYBridgeCallbacks
import org.ngbp.libatsc3.middleware.android.phy.models.BwPhyStatistics
import org.ngbp.libatsc3.middleware.android.phy.models.RfPhyStatistics
import org.ngbp.libatsc3.middleware.android.phy.virtual.PcapDemuxedVirtualPHYAndroid
import org.ngbp.libatsc3.middleware.android.phy.virtual.PcapSTLTPVirtualPHYAndroid
import org.ngbp.libatsc3.middleware.android.phy.virtual.srt.SRTRxSTLTPVirtualPHYAndroid
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

typealias MMTDataConsumerType = IMMTDataConsumer<MpuMetadata_HEVC_NAL_Payload, MfuByteBufferFragment>

internal class Atsc3Module(
        private val context: Context
) : IMMTDataProducer<MpuMetadata_HEVC_NAL_Payload, MfuByteBufferFragment>,
        IAtsc3NdkApplicationBridgeCallbacks, IAtsc3NdkPHYBridgeCallbacks, IAtsc3NdkMediaMMTBridgeCallbacks {

    enum class State {
        OPENED, PAUSED, IDLE
    }

    enum class PcapType {
        DEMUXED, STLTP
    }

    interface Listener {
        fun onStateChanged(state: State?)
        fun onServiceListTableReceived(services: List<Atsc3Service>, reportServerUrl: String?)
        fun onPackageReceived(appPackage: Atsc3Application)
        fun onCurrentServicePackageChanged(pkg: Atsc3HeldPackage?)
        fun onCurrentServiceDashPatched(mpdPath: String)
        fun onServiceGuideUnitReceived(filePath: String)
    }

    private val atsc3NdkApplicationBridge = Atsc3NdkApplicationBridge(this)
    private val atsc3NdkPHYBridge = Atsc3NdkPHYBridge(this)
    private val atsc3NdkMediaMMTBridge = Atsc3NdkMediaMMTBridge(this)

    private val usbManager by lazy { context.getSystemService(Context.USB_SERVICE) as UsbManager }

    private var atsc3NdkPHYClientInstance: Atsc3NdkPHYClientBase? = null //whomever is currently instantiated (e.g. SRTRxSTLTPVirtualPhyAndroid, etc..)
    private var atsc3NdkPHYClientFreqKhz: Int = 0

    private val serviceMap = ConcurrentHashMap<Int, Atsc3Service>()
    private val packageMap = HashMap<String, Atsc3Application>()

    private var state = State.IDLE
    private var selectedServiceId = -1
    private var selectedServiceSLSProtocol = -1
    private var selectedServiceHeld: Atsc3Held? = null
    private var selectedServicePackage: Atsc3HeldPackage? = null
    private var selectedServiceHeldXml: String? = null //TODO: use TOI instead

    @Volatile
    private var phyDemodLock: Boolean = false

    @Volatile
    private var mmtSource: MMTDataConsumerType? = null

    val slsProtocol: Int
        get() = selectedServiceSLSProtocol

    @Volatile
    private var listener: Listener? = null

    fun setListener(listener: Listener?) {
        if (this.listener != null) throw IllegalStateException("Atsc3Module listener already initialized")
        this.listener = listener
    }

    fun tune(freqKhz: Int, retuneOnDemod: Boolean) {
        val demodLock = phyDemodLock
        if (atsc3NdkPHYClientFreqKhz == freqKhz || (!retuneOnDemod && demodLock)) return

        atsc3NdkPHYClientInstance?.let { phy ->
            atsc3NdkPHYClientFreqKhz = freqKhz

            reset()

            phy.tune(freqKhz, 0)
        }
    }

    fun openPhy(phy: Atsc3NdkPHYClientBase, fd: Int, devicePath: String?, freqKhz: Int): Boolean {
        if (phy.init() == 0) {
            if (phy.open(fd, devicePath) == 0) {
                if (freqKhz > 0) {
                    phy.tune(freqKhz, 0)
                }

                close()
                atsc3NdkPHYClientInstance = phy

                return true
            }
        }

        return false
    }

    fun openPcapFile(filename: String, type: PcapType): Boolean {
        log("Opening PCAP file: $filename")

        close()

        atsc3NdkPHYClientInstance = when (type) {
            PcapType.DEMUXED -> PcapDemuxedVirtualPHYAndroid()
            PcapType.STLTP -> PcapSTLTPVirtualPHYAndroid()
        }.apply {
            init()
        }.also { client ->
            val res = client.open_from_capture(filename)

            //TODO: for assets mAt3DrvIntf.atsc3_pcap_open_for_replay_from_assetManager(filename, assetManager);
            if (res == RES_OK) {
                client.run()
                setState(State.OPENED)
            }
        }

        return true
    }

    fun openSRTStream(srtSource: String): Boolean {
        log("Opening SRT file: $srtSource")

        close()

        atsc3NdkPHYClientInstance = SRTRxSTLTPVirtualPHYAndroid().apply {
            init()
        }.also { client ->
            client.setSrtSourceConnectionString(srtSource)
            client.run()
        }

        setState(State.OPENED)

        return true
    }

    fun openUsbDevice(device: UsbDevice): Boolean {
        log("Opening USB device: ${device.deviceName}")

        val candidatePHYList = getPHYImplementations(device)
        if (candidatePHYList.isEmpty()) return false

        val conn = usbManager.openDevice(device) ?: return false

        close()

        val atsc3UsbDevice = Atsc3UsbDevice(device, conn)

        candidatePHYList.forEach { candidatePHY ->
            val atsc3NdkPHYClientBaseCandidate = Atsc3NdkPHYClientBase.CreateInstanceFromUSBVendorIDProductIDSupportedPHY(candidatePHY)

            if (candidatePHY.getIsBootloader(device)) {
                val r = atsc3NdkPHYClientBaseCandidate.download_bootloader_firmware(atsc3UsbDevice.fd, atsc3UsbDevice.deviceName)
                if (r < 0) {
                    log("prepareDevices: download_bootloader_firmware with $atsc3NdkPHYClientBaseCandidate failed for path: ${atsc3UsbDevice.deviceName}, fd: ${atsc3UsbDevice.fd}")
                } else {
                    log("prepareDevices: download_bootloader_firmware with $atsc3NdkPHYClientBaseCandidate for path: ${atsc3UsbDevice.deviceName}, fd: ${atsc3UsbDevice.fd}, success")
                    //pre-boot devices should re-enumerate, so don't track this connection just yet...
                }

            } else {
                val r = atsc3NdkPHYClientBaseCandidate.open(atsc3UsbDevice.fd, atsc3UsbDevice.deviceName)
                if (r < 0) {
                    log("prepareDevices: open with $atsc3NdkPHYClientBaseCandidate failed for path: ${atsc3UsbDevice.deviceName}, fd: ${atsc3UsbDevice.fd}, res: $r")
                } else {
                    log("prepareDevices: open with $atsc3NdkPHYClientBaseCandidate for path: ${atsc3UsbDevice.deviceName}, fd: ${atsc3UsbDevice.fd}, success")

                    atsc3NdkPHYClientBaseCandidate.setAtsc3UsbDevice(atsc3UsbDevice)
                    atsc3UsbDevice.setAtsc3NdkPHYClientBase(atsc3NdkPHYClientBaseCandidate)

                    atsc3NdkPHYClientInstance = atsc3NdkPHYClientBaseCandidate
                    setState(State.OPENED)
                    return true
                }
            }
        }

        atsc3UsbDevice.destroy()

        return false
    }

    private var tmpAdditionalServiceOpened = false

    fun selectService(serviceId: Int): Boolean {
        if (selectedServiceId == serviceId) return false

        clearHeld()
        setMMTSource(null)

        selectedServiceId = serviceId
        selectedServiceSLSProtocol = atsc3NdkApplicationBridge.atsc3_slt_selectService(serviceId)

        //TODO: temporary test solution
        if (!tmpAdditionalServiceOpened) {
            serviceMap.values.firstOrNull {
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
        atsc3NdkPHYClientInstance?.stop()

        setState(State.PAUSED)
    }

    fun close() {
        atsc3NdkPHYClientInstance?.let { client ->
            log("closeUsbDevice -- calling client.deinit")

            client.deinit()

            client.atsc3UsbDevice?.let { device ->
                log("closeUsbDevice -- before FindFromUsbDevice")
                Atsc3UsbDevice.DumpAllAtsc3UsbDevices()

                device.destroy()

                Atsc3UsbDevice.DumpAllAtsc3UsbDevices()
            }
            atsc3NdkPHYClientInstance = null
        }

        atsc3NdkPHYClientFreqKhz = 0

        reset()
    }

    private fun reset() {
        setState(State.IDLE)
        clear()
    }

    private fun getPHYImplementations(device: UsbDevice): List<Atsc3NdkPHYClientBase.USBVendorIDProductIDSupportedPHY> {
        Atsc3UsbDevice.FindFromUsbDevice(device)?.let {
            log("usbPHYLayerDeviceTryToInstantiateFromRegisteredPHYNDKs: Atsc3UsbDevice already instantiated: $device, instance: $it")
            return emptyList()
        } ?: log("usbPHYLayerDeviceTryToInstantiateFromRegisteredPHYNDKs: Atsc3UsbDevice map returned : $device, but null instance?")

        return Atsc3NdkPHYClientBase.GetCandidatePHYImplementations(device) ?: emptyList()
    }

    override fun setMMTSource(source: MMTDataConsumerType?) {
        ATSC3PlayerFlags.ATSC3PlayerStartPlayback = false

        mmtSource?.let { oldSource ->
            oldSource.release()
            mmtSource = null
        }

        mmtSource = source
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
        serviceMap.clear()
        setMMTSource(null)

        //TODO: temporary test solution
        if (tmpAdditionalServiceOpened) {
            atsc3NdkApplicationBridge.atsc3_slt_alc_clear_additional_service_selections()
            tmpAdditionalServiceOpened = false
        }
    }

    private fun clearService() {
        selectedServiceId = -1
        selectedServiceSLSProtocol = -1
    }

    private fun clearHeld() {
        selectedServiceHeld = null
        selectedServicePackage = null
        selectedServiceHeldXml = null
    }

    //////////////////////////////////////////////////////////////
    /// IAtsc3NdkApplicationBridgeCallbacks
    //////////////////////////////////////////////////////////////

    override fun showMsgFromNative(message: String) = log(message)

    override fun jni_getCacheDir(): File = context.cacheDir

    override fun onSlsTablePresent(sls_payload_xml: String) {
        log("onSlsTablePresent, $sls_payload_xml")

        LLSParserSLT().parseXML(sls_payload_xml).let { (services, urls) ->
            serviceMap.putAll(services.map { it.serviceId to it }.toMap())
            listener?.onServiceListTableReceived(
                    Collections.unmodifiableList(services),
                    urls[SLTConstants.URL_TYPE_REPORT_SERVER]
            )
        }
    }

    override fun onAeatTablePresent(aeatPayloadXML: String) {
        //TODO("Not yet implemented")
    }

    override fun onSlsHeldEmissionPresent(serviceId: Int, heldPayloadXML: String) {
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
        when (s_tsid_content_type) {
            CONTENT_TYPE_SGDD -> {
                // skip
            }

            CONTENT_TYPE_SGDU -> {
                cache_file_path?.let {
                    listener?.onServiceGuideUnitReceived(getFullPath(cache_file_path))
                }
            }
        }
    }

    override fun onPackageExtractCompleted(packageMetadata: PackageExtractEnvelopeMetadataAndPayload) {
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
        phyDemodLock = rfPhyStatistics.demod_lock_status != 0
    }

    override fun pushBwPhyStatistics(bwPhyStatistics: BwPhyStatistics) {
        // ignore
    }

    //////////////////////////////////////////////////////////////
    /// IAtsc3NdkMediaMMTBridgeCallbacks
    //////////////////////////////////////////////////////////////

    override fun pushMfuByteBufferFragment(mfuByteBufferFragment: MfuByteBufferFragment) {
        execIfMMTSourceIsActiveOrCancel({ source ->
            source.PushMfuByteBufferFragment(mfuByteBufferFragment)
        }, {
            mfuByteBufferFragment.unreferenceByteBuffer()
        })
    }

    override fun pushMpuMetadata_HEVC_NAL_Payload(mpuMetadata_hevc_nal_payload: MpuMetadata_HEVC_NAL_Payload) {
        execIfMMTSourceIsActiveOrCancel({ source ->
            source.InitMpuMetadata_HEVC_NAL_Payload(mpuMetadata_hevc_nal_payload)
        }, {
            mpuMetadata_hevc_nal_payload.releaseByteBuffer()
        })
    }

    override fun pushAudioDecoderConfigurationRecord(mmtAudioDecoderConfigurationRecord: MMTAudioDecoderConfigurationRecord?) {
        // ignore
    }

    //////////////////////////////////////////////////////////////

    private fun execIfMMTSourceIsActiveOrCancel(exec: (MMTDataConsumerType) -> Unit, cancel: () -> Unit = {}) {
        mmtSource?.let { source ->
            if (source.isActive()) {
                if (!ATSC3PlayerFlags.ATSC3PlayerStartPlayback) {
                    ATSC3PlayerFlags.ATSC3PlayerStartPlayback = true
                }

                exec.invoke(source)
            } else if (ATSC3PlayerFlags.ATSC3PlayerStartPlayback) {
                ATSC3PlayerFlags.ATSC3PlayerStartPlayback = false

                cancel.invoke()
            }
        } ?: cancel.invoke()
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

    private fun setState(newState: State) {
        state = newState
        listener?.onStateChanged(newState)
    }

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

        private const val RES_OK = 0

        const val SLS_PROTOCOL_DASH = 1
        const val SLS_PROTOCOL_MMT = 2
    }
}