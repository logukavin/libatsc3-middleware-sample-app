package com.nextgenbroadcast.mobile.middleware

import android.os.Handler
import android.os.Message
import com.nextgenbroadcast.mobile.core.model.*

class Atsc3ActivityIncomingHandler(
        private val selectorPresenter: InterprocessServiceBinder.SelectorPresenter,
        private val receiverPresenter: InterprocessServiceBinder.ReceiverPresenter,
        private val userAgentPresenter: InterprocessServiceBinder.UserAgentPresenter,
        private val mediaPlayerPresenter: InterprocessServiceBinder.MediaPlayerPresenter,
        private val incomingDataListener: OnIncomingPlayerStateListener
) : Handler() {

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what) {

            InterprocessServiceBinder.LIVEDATA_RECEIVER_STATE -> {
                with(msg.data) {
                    classLoader = ReceiverState::class.java.classLoader
                    getParcelable<ReceiverState>(InterprocessServiceBinder.PARAM_RECEIVER_STATE)?.let {
                        receiverPresenter.receiverState.postValue(it)
                    }
                }
            }

            InterprocessServiceBinder.LIVEDATA_SERVICE_LIST -> {
                with(msg.data) {
                    classLoader = SLSService::class.java.classLoader
                    getParcelableArrayList<SLSService>(InterprocessServiceBinder.PARAM_SERVICE_LIST)?.let { it ->
                        selectorPresenter.sltServices.postValue(it)
                    }
                }
            }

            InterprocessServiceBinder.LIVEDATA_SERVICE_SELECTED -> {
                with(msg.data) {
                    classLoader = SLSService::class.java.classLoader
                    val selectedService = getParcelable<SLSService?>(InterprocessServiceBinder.PARAM_SERVICE_SELECTED)
                    selectorPresenter.selectedService.postValue(selectedService)
                }
            }

            InterprocessServiceBinder.LIVEDATA_APPDATA -> {
                with(msg.data) {
                    classLoader = AppData::class.java.classLoader
                    val appData = getParcelable<AppData?>(InterprocessServiceBinder.PARAM_APPDATA)
                    userAgentPresenter.appData.postValue(appData)
                }
            }

            InterprocessServiceBinder.LIVEDATA_RMP_LAYOUT_PARAMS -> {
                with(msg.data) {
                    classLoader = RPMParams::class.java.classLoader
                    getParcelable<RPMParams>(InterprocessServiceBinder.PARAM_RMP_LAYOUT_PARAMS)?.let {
                        mediaPlayerPresenter.rmpLayoutParams.postValue(it)
                    }
                }
            }

            InterprocessServiceBinder.LIVEDATA_RMP_MEDIA_URL -> {
                msg.data.getString(InterprocessServiceBinder.PARAM_RMP_MEDIA_URL)?.let {
                    mediaPlayerPresenter.rmpMediaUrl.postValue(it)
                }
            }

            InterprocessServiceBinder.ACTION_PLAYER_STATE_CHANGE_PAUSE -> {
                incomingDataListener.onPlayerStatePause()
            }

            InterprocessServiceBinder.ACTION_PLAYER_STATE_CHANGE_RESUME -> {
                incomingDataListener.onPlayerStateResume()
            }

        }
    }
}