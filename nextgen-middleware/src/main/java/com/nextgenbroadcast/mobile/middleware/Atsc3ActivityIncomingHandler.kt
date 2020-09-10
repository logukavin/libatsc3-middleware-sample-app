package com.nextgenbroadcast.mobile.middleware

import android.os.Handler
import android.os.Message
import com.nextgenbroadcast.mobile.core.model.*

class Atsc3ActivityIncomingHandler(
        private val callback: OnIncomingDataListener
) : Handler() {

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what) {

            InterprocessServiceBinder.LIVEDATA_RECEIVER_STATE -> {
                msg.data.classLoader = ReceiverState::class.java.classLoader
                msg.data.getParcelable<ReceiverState>(InterprocessServiceBinder.PARAM_RECEIVER_STATE)?.let {
                    callback.onReceiverState(it)
                }
            }

            InterprocessServiceBinder.LIVEDATA_SERVICE_LIST -> {
                msg.data.classLoader = SLSService::class.java.classLoader
                msg.data.getParcelableArrayList<SLSService>(InterprocessServiceBinder.PARAM_SERVICE_LIST)?.let { it ->
                    callback.onSLSServices(it)
                }
            }

            InterprocessServiceBinder.LIVEDATA_SERVICE_SELECTED -> {
                msg.data.classLoader = SLSService::class.java.classLoader
                val selectedService = msg.data.getParcelable<SLSService?>(InterprocessServiceBinder.PARAM_SERVICE_SELECTED)
                callback.onSelectSLSService(selectedService)
            }

            InterprocessServiceBinder.LIVEDATA_APPDATA -> {
                msg.data.classLoader = AppData::class.java.classLoader
                val appData = msg.data.getParcelable<AppData?>(InterprocessServiceBinder.PARAM_APPDATA)
                callback.onAppData(appData)
            }

            InterprocessServiceBinder.LIVEDATA_RMP_LAYOUT_PARAMS -> {
                msg.data.classLoader = RPMParams::class.java.classLoader
                msg.data.getParcelable<RPMParams>(InterprocessServiceBinder.PARAM_RMP_LAYOUT_PARAMS)?.let {
                    callback.onRPMParams(it)
                }
            }

            InterprocessServiceBinder.LIVEDATA_RMP_MEDIA_URL -> {
                msg.data.getString(InterprocessServiceBinder.PARAM_RMP_MEDIA_URL)?.let {
                    callback.onRPMMediaUrl(it)
                }
            }

            InterprocessServiceBinder.ACTION_PLAYER_STATE_CHANGE_PAUSE -> {
                callback.onPlayerStatePause()
            }

            InterprocessServiceBinder.ACTION_PLAYER_STATE_CHANGE_RESUME -> {
                callback.onPlayerStateResume()
            }

        }
    }
}