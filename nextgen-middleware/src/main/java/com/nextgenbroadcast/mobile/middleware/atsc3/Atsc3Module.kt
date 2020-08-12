package com.nextgenbroadcast.mobile.middleware.atsc3

import android.content.Context
import android.util.Log
import com.nextgenbroadcast.mobile.middleware.BuildConfig
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3ApplicationFile
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3Held
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.HeldXmlParser
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.service.Atsc3Service
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.service.LLSParserSLT
import com.nextgenbroadcast.mobile.middleware.atsc3.ndk.Atsc3UsbDevice
import com.nextgenbroadcast.mobile.middleware.atsc3.ndk.atsc3NdkClient
import com.nextgenbroadcast.mobile.middleware.atsc3.ndk.atsc3NdkClient.ClientListener
import com.nextgenbroadcast.mobile.middleware.atsc3.ndk.data.PackageExtractEnvelopeMetadataAndPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap

//TODO: multithreading requests
internal class Atsc3Module(context: Context) : ClientListener {
    enum class State {
        OPENED, PAUSED, IDLE
    }

    interface Listener {
        fun onStateChanged(state: State?)
        fun onServicesLoaded(services: List<Atsc3Service?>)
        fun onPackageReceived(appPackage: Atsc3Application)
        fun onCurrentServicePackageChanged(pkg: Atsc3HeldPackage?)
        fun onCurrentServiceDashPatched(mpdPath: String)
    }

    private val client = atsc3NdkClient(context.cacheDir, this)
    private var usbDevice: Atsc3UsbDevice? = null

    private val serviceMap = ConcurrentHashMap<Int, Atsc3Service>()
    private val packageMap = HashMap<String, Atsc3Application>()

    private var isPcapOpened = false
    private var state = State.IDLE
    private var selectedServiceId = -1
    private var selectedServiceSLSProtocol = -1
    private var selectedServiceHeld: Atsc3Held? = null
    private var selectedServicePackage: Atsc3HeldPackage? = null
    private var selectedServiceHeldXml: String? = null //TODO: use TOI instead

    @Volatile
    private var listener: Listener? = null

    init {
        with(client) {
            ApiInit(client)
            if (BuildConfig.DEBUG) {
                setLogListener { s: String -> log("Client log: %s", s) }
            }
        }
    }

    fun setListener(listener: Listener?) {
        if (this.listener != null) throw IllegalStateException("Atsc3Module listener already initialized")
        this.listener = listener
    }

    fun openPcapFile(filename: String?): Boolean {
        if (filename.isNullOrEmpty()) return false

        log("Opening pcap file: %s", filename)
        val res = client.atsc3_pcap_open_for_replay(filename)

        //TODO: for assets mAt3DrvIntf.atsc3_pcap_open_for_replay_from_assetManager(filename, assetManager);
        if (res == RES_OK) {
            isPcapOpened = true
            client.atsc3_pcap_thread_run()
            setState(State.OPENED)
            return true
        }

        return false
    }

    fun openUsbDevice(device: Atsc3UsbDevice): Boolean {
        if (usbDevice != null) {
            client.ApiClose();
        }

        usbDevice = device
        client.atsc3_pcap_thread_stop()
        setState(State.IDLE)

        log("opening Device: fd: %d, key: %d", device.fd, device.key)

        //TODO: start in thread
        CoroutineScope(Dispatchers.IO).launch {
            val re = client.ApiOpen(device.fd, device.key)
            withContext(Dispatchers.Main) {
                if (re < 0) {
                    log("open: failed, r: %d", re);
                } else if (re == 240) { //SL_FX3S_I2C_AWAITING_BROADCAST_USB_ATTACHED
                    log("open: pending SL_FX3S_I2C_AWAITING_BROADCAST_USB_ATTACHED event");
                } else {
                    setState(State.OPENED)
                }
            }
        }

//
//        stopAllPlayers();
//

        return true
    }

    fun selectService(serviceId: Int): Boolean {
        clearHeld()

        selectedServiceId = serviceId
        selectedServiceSLSProtocol = client.atsc3_slt_selectService(serviceId)

        return selectedServiceSLSProtocol > 0
    }

    fun stop() {
        if (isPcapOpened) {
            isPcapOpened = false
            client.atsc3_pcap_thread_stop()
            clear()
            setState(State.IDLE)
            log("pcap thread stopped")
            return
        }

        if (usbDevice == null) {
            log("no atlas device connected yet")
            return
        }
        client.ApiStop()
        setState(State.PAUSED)
    }

    fun close() {
        usbDevice?.disconnect() ?: return

        clear()

        val status = client.ApiClose()
        if (status != RES_OK) {
            log("closed")
        }

        setState(State.IDLE)
    }

    private fun getSelectedServiceMediaUri(): String? {
        var mediaUri: String? = null
        if (selectedServiceSLSProtocol == 1) {
            val routeMPDFileName = client.atsc3_slt_alc_get_sls_metadata_fragments_content_locations_from_monitor_service_id(selectedServiceId, DASH_CONTENT_TYPE)
            if (routeMPDFileName.isNotEmpty()) {
                mediaUri = String.format("%s/%s", client.cacheDir, routeMPDFileName[0])
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

    override fun onSlsTablePresent(sls_payload_xml: String) {
        val services = LLSParserSLT().parseXML(sls_payload_xml)

        serviceMap.putAll(services.map { it.serviceId to it }.toMap())

        listener?.onServicesLoaded(Collections.unmodifiableList(services))
    }

    override fun onSlsHeldReceived(service_id: Int, held_payload_xml: String) {
        if (service_id == selectedServiceId) {
            if (held_payload_xml != selectedServiceHeldXml) {
                selectedServiceHeldXml = held_payload_xml

                val held = HeldXmlParser().parseXML(held_payload_xml).also { held ->
                    selectedServiceHeld = held
                }

                if (held != null) {
                    val pkg = held.findActivePackage(service_id)

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

    override fun onPackageExtractCompleted(packageMetadata: PackageExtractEnvelopeMetadataAndPayload) {
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

    private fun metadataToPackage(packageMetadata: PackageExtractEnvelopeMetadataAndPayload): Atsc3Application {
        val files = packageMetadata.multipartRelatedPayloadList?.map { file ->
            file.contentLocation to Atsc3ApplicationFile(file.contentLocation, file.contentType, file.version)
        }?.toMap() ?: emptyMap<String, Atsc3ApplicationFile>()

        return Atsc3Application(
                packageMetadata.appContextIdList.split(" "),
                String.format("%s/%s", client.cacheDir, packageMetadata.packageExtractPath),
                files
        )
    }

    override fun onRouteDashUpdated(service_id: Int) {
        if (service_id == selectedServiceId) {
            getSelectedServiceMediaUri()?.let { mpdPath ->
                listener?.onCurrentServiceDashPatched(mpdPath)
            }
        }
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
    }
}