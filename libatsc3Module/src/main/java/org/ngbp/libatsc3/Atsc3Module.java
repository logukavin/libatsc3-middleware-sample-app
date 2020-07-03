package org.ngbp.libatsc3;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.ngbp.libatsc3.ndk.atsc3NdkClient;
import org.ngbp.libatsc3.ndk.Atsc3UsbDevice;
import org.ngbp.libatsc3.ndk.a331.LLSParserSLT;
import org.ngbp.libatsc3.ndk.a331.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Atsc3Module implements atsc3NdkClient.ClientListener {
    public static final String TAG = Atsc3Module.class.getSimpleName();

    private static final String DASH_CONTENT_TYPE = "application/dash+xml";

    private static final int RES_OK = 0;

    public enum State {
        OPENED, PAUSED, IDLE
    }

    private final atsc3NdkClient client;

    private Atsc3UsbDevice usbDevice = null;

    private boolean isPcapOpened;

    private MutableLiveData<List<Service>> sltServices = new MutableLiveData<>();
    private MutableLiveData<Uri> serviceMediaUri = new MutableLiveData<>();
    private MutableLiveData<State> state = new MutableLiveData<>();

    public Atsc3Module(@NonNull Context context) {
        client = new atsc3NdkClient(context.getCacheDir(), this);
        client.ApiInit(client);

        if (BuildConfig.DEBUG) {
            client.setLogListener((s) -> {
                log("Client log: %s", s);
            });
        }
    }

    public LiveData<List<Service>> getSltServices() {
        return sltServices;
    }

    public LiveData<Uri> getServiceMediaUri() {
        return serviceMediaUri;
    }

    public LiveData<State> getState() {
        return state;
    }

    public boolean openPcapFile(String filename) {
        if (TextUtils.isEmpty(filename)) return false;

        log("Opening pcap file: %s", filename);

        int res = client.atsc3_pcap_open_for_replay(filename);
        //TODO: for assets mAt3DrvIntf.atsc3_pcap_open_for_replay_from_assetManager(filename, assetManager);

        if (res == RES_OK) {
            isPcapOpened = true;

            client.atsc3_pcap_thread_run();

            setState(State.OPENED);

            return true;
        }

        return false;
    }

    public boolean openUsb() {
        if (usbDevice == null) {
            log("no FX3 device connected yet");
            return false;
        }

        //notify pcap thread (if running) to stop
        client.atsc3_pcap_thread_stop();

        log("opening mCurFx3Device: fd: %d, key: %d", usbDevice.fd, usbDevice.key);
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

        return false;
    }

    @Nullable
    public void selectService(Service service) {
        final int serviceId = service.getServiceId();

        int selectedServiceSLSProtocol = client.atsc3_slt_selectService(serviceId);

        Uri mediaUri = null;
        if (selectedServiceSLSProtocol == 1) {
            String[] routeMPDFileName = client.atsc3_slt_alc_get_sls_metadata_fragments_content_locations_from_monitor_service_id(serviceId, DASH_CONTENT_TYPE);
            if (routeMPDFileName.length > 0) {
                mediaUri = Uri.parse(String.format("%s/%s", client.getCacheDir(), routeMPDFileName[0]));
            } else {
                log("Unable to resolve Dash MPD path from MBMS envelope, service_id: %d", serviceId);
            }
        } /*else if (selectedServiceSLSProtocol == 2) {
            //TODO: add support
        }*/ else {
            log("unsupported service protocol: %d", selectedServiceSLSProtocol);
        }

        serviceMediaUri.postValue(mediaUri);
    }

    public void stop() {
        if (isPcapOpened) {
            isPcapOpened = false;
            client.atsc3_pcap_thread_stop();

            setState(State.IDLE);

            log("pcap thread stopped");
            return;
        }

        if (usbDevice == null) {
            log("no atlas device connected yet");
            return;
        }

        client.ApiStop();

        setState(State.PAUSED);
    }

    public void close() {
        if (usbDevice == null) {
            log("no atlas device connected yet");
            return;
        }

        int status = client.ApiClose();
        if (status != RES_OK) {
            log("closed");
        }

        setState(State.IDLE);
    }

    @Override
    public void onSlsTablePresent(String sls_payload_xml) {
        LLSParserSLT llsParserSLT = new LLSParserSLT();
        ArrayList<Service> services = llsParserSLT.parseXML(sls_payload_xml);

        sltServices.postValue(Collections.unmodifiableList(services));
    }

    @Override
    public void onAlcObjectStatusMessage(String alc_object_status_message) {
        //TODO: notify value changed
    }

    private void setState(State newState) {
        state.postValue(newState);
    }

    private void log(String text, Object... params) {
        if (params.length > 0) {
            text = String.format(Locale.US, text, params);
        }
        Log.d(TAG, text);
    }

}
