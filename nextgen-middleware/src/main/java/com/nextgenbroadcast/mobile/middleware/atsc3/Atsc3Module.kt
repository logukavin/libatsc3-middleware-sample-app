package com.nextgenbroadcast.mobile.middleware.atsc3

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.nextgenbroadcast.mobile.core.media.IMMTDataConsumer
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3ApplicationFile
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3Held
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.HeldXmlParser
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.service.Atsc3Service
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.service.LLSParserSLT
import com.nextgenbroadcast.mobile.core.media.IMMTDataProducer
import org.ngbp.libatsc3.middleware.Atsc3NdkApplicationBridge
import org.ngbp.libatsc3.middleware.android.a331.PackageExtractEnvelopeMetadataAndPayload
import org.ngbp.libatsc3.middleware.android.application.interfaces.IAtsc3NdkApplicationBridgeCallbacks
import org.ngbp.libatsc3.middleware.android.application.sync.mmt.MfuByteBufferFragment
import org.ngbp.libatsc3.middleware.android.application.sync.mmt.MpuMetadata_HEVC_NAL_Payload
import org.ngbp.libatsc3.middleware.android.phy.Atsc3NdkPHYClientBase
import org.ngbp.libatsc3.middleware.android.phy.Atsc3UsbDevice
import org.ngbp.libatsc3.middleware.android.phy.virtual.PcapDemuxedVirtualPHYAndroid
import org.ngbp.libatsc3.middleware.android.phy.virtual.PcapSTLTPVirtualPHYAndroid
import org.ngbp.libatsc3.middleware.android.phy.virtual.srt.SRTRxSTLTPVirtualPHYAndroid
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

typealias MMTDataConsumerType = IMMTDataConsumer<MpuMetadata_HEVC_NAL_Payload, MfuByteBufferFragment>

//TODO: multithreading requests
internal class Atsc3Module(
        private val context: Context
) : IMMTDataProducer<MpuMetadata_HEVC_NAL_Payload, MfuByteBufferFragment>, IAtsc3NdkApplicationBridgeCallbacks {

    enum class State {
        OPENED, PAUSED, IDLE
    }

    enum class PcapType {
        DEMUXED, STLTP
    }

    interface Listener {
        fun onStateChanged(state: State?)
        fun onServicesLoaded(services: List<Atsc3Service?>)
        fun onPackageReceived(appPackage: Atsc3Application)
        fun onCurrentServicePackageChanged(pkg: Atsc3HeldPackage?)
        fun onCurrentServiceDashPatched(mpdPath: String)
    }

    private val atsc3NdkApplicationBridge = Atsc3NdkApplicationBridge(this)

    private val usbManager by lazy { context.getSystemService(Context.USB_SERVICE) as UsbManager }

    private var atsc3NdkPHYClientInstance: Atsc3NdkPHYClientBase? = null //whomever is currently instantiated (e.g. SRTRxSTLTPVirtualPhyAndroid, etc..)

    private val serviceMap = ConcurrentHashMap<Int, Atsc3Service>()
    private val packageMap = HashMap<String, Atsc3Application>()

    private var state = State.IDLE
    private var selectedServiceId = -1
    private var selectedServiceSLSProtocol = -1
    private var selectedServiceHeld: Atsc3Held? = null
    private var selectedServicePackage: Atsc3HeldPackage? = null
    private var selectedServiceHeldXml: String? = null //TODO: use TOI instead
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

        Atsc3UsbDevice.DumpAllAtsc3UsbDevices()

        Atsc3UsbDevice.FindFromUsbDevice(device)?.let {
            log("usbPHYLayerDeviceTryToInstantiateFromRegisteredPHYNDKs: Atsc3UsbDevice already instantiated: $device, instance: $it")
            return false
        } ?: log("usbPHYLayerDeviceTryToInstantiateFromRegisteredPHYNDKs: Atsc3UsbDevice map returned : $device, but null instance?")

        close()

        val candidatePHYList = Atsc3NdkPHYClientBase.GetCandidatePHYImplementations(device)
                ?: return false

        val conn = usbManager.openDevice(device) ?: return false

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
                    //jjustman-2020-08-31 - hack for LowaSIS - tune to 593000 - CH34
                    atsc3NdkPHYClientBaseCandidate.tune(593000, 0)

                    atsc3NdkPHYClientInstance = atsc3NdkPHYClientBaseCandidate
                    setState(State.OPENED)
                    return true
                }
            }
        }

        atsc3UsbDevice.destroy()

        return false
    }

    fun selectService(serviceId: Int): Boolean {
        clearHeld()

        selectedServiceId = serviceId
        selectedServiceSLSProtocol = atsc3NdkApplicationBridge.atsc3_slt_selectService(serviceId)

        return selectedServiceSLSProtocol > 0
    }

    fun stop() {
        atsc3NdkPHYClientInstance?.stop()

        setState(State.PAUSED)
    }

    fun close() {
        atsc3NdkPHYClientInstance?.let { client ->
            client.atsc3UsbDevice?.let { device ->
                log("closeUsbDevice -- before FindFromUsbDevice")
                Atsc3UsbDevice.DumpAllAtsc3UsbDevices();

                device.destroy()

                Atsc3UsbDevice.DumpAllAtsc3UsbDevices();
            }
            client.stop()
            client.deinit()
//            try {
//                Thread.sleep(1000)
//            } catch (e: InterruptedException) {
//                e.printStackTrace()
//            }
            atsc3NdkPHYClientInstance = null
        }

        clear()

        setState(State.IDLE)
    }

    override fun setMMTSource(source: MMTDataConsumerType) {
        mmtSource = source
    }

    override fun resetMMTSource(source: MMTDataConsumerType) {
        if (mmtSource == source) {
            mmtSource = null
        }
    }

    private fun getSelectedServiceMediaUri(): String? {
        var mediaUri: String? = null
        if (selectedServiceSLSProtocol == SLS_PROTOCOL_DASH) {
            val routeMPDFileName = atsc3NdkApplicationBridge.atsc3_slt_alc_get_sls_metadata_fragments_content_locations_from_monitor_service_id(selectedServiceId, DASH_CONTENT_TYPE)
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
        mmtSource = null
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
        log("onSlsTablePresent, $sls_payload_xml");

        val services = LLSParserSLT().parseXML(sls_payload_xml)

        serviceMap.putAll(services.map { it.serviceId to it }.toMap())

        listener?.onServicesLoaded(Collections.unmodifiableList(services))
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

    override fun pushMfuByteBufferFragment(mfuByteBufferFragment: MfuByteBufferFragment) {
        mmtSource?.PushMfuByteBufferFragment(mfuByteBufferFragment)
    }

    override fun pushMpuMetadata_HEVC_NAL_Payload(mpuMetadata_hevc_nal_payload: MpuMetadata_HEVC_NAL_Payload) {
        mmtSource?.InitMpuMetadata_HEVC_NAL_Payload(mpuMetadata_hevc_nal_payload)
    }

    override fun onAlcObjectStatusMessage(alc_object_status_message: String) {
        //TODO: notify value changed
    }

    override fun onPackageExtractCompleted(packageMetadata: PackageExtractEnvelopeMetadataAndPayload) {
        log("onPackageExtractCompleted with packageMetadata.appContextIdList: ${packageMetadata.appContextIdList}")

        val appPackage = packageMap[packageMetadata.appContextIdList]
        if (appPackage == null) {
            val pkg = metadataToPackage(packageMetadata).also {
                packageMap[packageMetadata.appContextIdList] = it
            }
            listener?.onPackageReceived(pkg)
        } else {
            val changedFiles = packageMetadata.multipartRelatedPayloadList.filter { file ->
                appPackage.files[file.contentLocation]?.version != file.version
            }.map { file ->
                Atsc3ApplicationFile(file.contentLocation, file.contentType, file.version)
            }

            if (changedFiles.isNotEmpty()) {
                val pkg = appPackage.updateFiles(changedFiles).also {
                    packageMap[packageMetadata.appContextIdList] = it
                }
                listener?.onPackageReceived(pkg)
            }
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

    private fun metadataToPackage(packageMetadata: PackageExtractEnvelopeMetadataAndPayload): Atsc3Application {
        val files = packageMetadata.multipartRelatedPayloadList?.map { file ->
            file.contentLocation to Atsc3ApplicationFile(file.contentLocation, file.contentType, file.version)
        }?.toMap() ?: emptyMap<String, Atsc3ApplicationFile>()

        return Atsc3Application(
                packageMetadata.appContextIdList.split(" "),
                String.format("%s/%s", jni_getCacheDir(), packageMetadata.packageExtractPath),
                files
        )
    }

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

        private const val DASH_CONTENT_TYPE = "application/dash+xml"
        private const val RES_OK = 0

        const val SLS_PROTOCOL_DASH = 1
        const val SLS_PROTOCOL_MMT = 2
    }
}