package org.ngbp.libatsc3

import android.content.Context
import android.net.Uri
import android.util.Log
import org.ngbp.libatsc3.ndk.Atsc3UsbDevice
import org.ngbp.libatsc3.ndk.a331.LLSParserSLT
import org.ngbp.libatsc3.ndk.a331.Service
import org.ngbp.libatsc3.ndk.atsc3NdkClient
import org.ngbp.libatsc3.ndk.atsc3NdkClient.ClientListener
import java.lang.IllegalStateException
import java.util.*

//TODO: multithreading requests
class Atsc3Module(context: Context) : ClientListener {
    enum class State {
        OPENED, PAUSED, IDLE
    }

    interface Listener {
        fun onStateChanged(state: State?)
        fun onServicesLoaded(services: List<Service?>)
    }

    private val client: atsc3NdkClient = atsc3NdkClient(context.cacheDir, this)
    private val usbDevice: Atsc3UsbDevice? = null

    private var isPcapOpened = false
    private var state = State.IDLE
    private var selectedServiceId = -1
    private var selectedServiceSLSProtocol = -1

    @Volatile
    private var listener: Listener? = null

    init {
        client.ApiInit(client)
        if (BuildConfig.DEBUG) {
            client.setLogListener { s: String -> log("Client log: %s", s) }
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

    fun openUsb(): Boolean {
        if (usbDevice == null) {
            log("no FX3 device connected yet")
            return false
        }

        //notify pcap thread (if running) to stop
        client.atsc3_pcap_thread_stop()
        log("opening mCurFx3Device: fd: %d, key: %d", usbDevice.fd, usbDevice.key)
        //        //ThingsUI.WriteToAlphaDisplayNoEx("OPEN");
//
//        stopAllPlayers();
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                int re = mAt3DrvIntf.ApiOpen(mCurAt3Device.fd, mCurAt3Device.key);
//                if (re < 0) {
//                    log("open: failed, r: %d", re);
//                } else if(re == 240) { //SL_FX3S_I2C_AWAITING_BROADCAST_USB_ATTACHED
//                    log("open: pending SL_FX3S_I2C_AWAITING_BROADCAST_USB_ATTACHED event");
//                }
//            }
//        }).start();
        return false
    }

    fun selectService(service: Service): Boolean {
        selectedServiceId = service.serviceId.also { serviceId ->
            selectedServiceSLSProtocol = client.atsc3_slt_selectService(serviceId)
        }

        return selectedServiceSLSProtocol > 0
    }

    fun stop() {
        if (isPcapOpened) {
            isPcapOpened = false
            client.atsc3_pcap_thread_stop()
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
        if (usbDevice == null) {
            log("no atlas device connected yet")
            return
        }

        val status = client.ApiClose()
        if (status != RES_OK) {
            log("closed")
        }

        setState(State.IDLE)
    }

    fun getSelectedServiceMediaUri(): Uri? {
        var mediaUri: Uri? = null
        if (selectedServiceSLSProtocol == 1) {
            val routeMPDFileName = client.atsc3_slt_alc_get_sls_metadata_fragments_content_locations_from_monitor_service_id(selectedServiceId, DASH_CONTENT_TYPE)
            if (routeMPDFileName.isNotEmpty()) {
                mediaUri = Uri.parse(String.format("%s/%s", client.cacheDir, routeMPDFileName[0]))
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

    override fun onSlsTablePresent(sls_payload_xml: String) {
        val services = LLSParserSLT().parseXML(sls_payload_xml)
        listener?.onServicesLoaded(Collections.unmodifiableList(services))
    }

    override fun onAlcObjectStatusMessage(alc_object_status_message: String) {
        //TODO: notify value changed
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