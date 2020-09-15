package com.nextgenbroadcast.mobile.middleware.service.handler

import android.os.Handler
import android.os.Message
import com.nextgenbroadcast.mobile.core.model.*
import com.nextgenbroadcast.mobile.middleware.service.binder.InterprocessServiceBinder

class Atsc3ActivityIncomingHandler(
        private val selectorPresenter: InterprocessServiceBinder.SelectorPresenter,
        private val receiverPresenter: InterprocessServiceBinder.ReceiverPresenter,
        private val userAgentPresenter: InterprocessServiceBinder.UserAgentPresenter,
        private val mediaPlayerPresenter: InterprocessServiceBinder.MediaPlayerPresenter,
        private val incomingDataListener: OnIncomingPlayerStateListener
) : Handler() {

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when (msg.what) {

            InterprocessServiceBinder.LIVEDATA_RECEIVER_STATE -> {
                msg.data.getParcelable(ReceiverState::class.java, InterprocessServiceBinder.PARAM_RECEIVER_STATE)?.let {
                    receiverPresenter.receiverState.postValue(it)
                }
            }

            InterprocessServiceBinder.LIVEDATA_SERVICE_LIST -> {
                msg.data.getParcelableArrayList(SLSService::class.java, InterprocessServiceBinder.PARAM_SERVICE_LIST)?.let {
                    selectorPresenter.sltServices.postValue(it)
                }
            }

            InterprocessServiceBinder.LIVEDATA_SERVICE_SELECTED -> {
                val selectedService = msg.data.getParcelable(SLSService::class.java, InterprocessServiceBinder.PARAM_SERVICE_SELECTED)
                selectorPresenter.selectedService.postValue(selectedService)
            }

            InterprocessServiceBinder.LIVEDATA_APPDATA -> {
                val appData = msg.data.getParcelable(AppData::class.java, InterprocessServiceBinder.PARAM_APPDATA)
                userAgentPresenter.appData.postValue(appData)
            }

            InterprocessServiceBinder.LIVEDATA_RMP_LAYOUT_PARAMS -> {
                msg.data.getParcelable(RPMParams::class.java, InterprocessServiceBinder.PARAM_RMP_LAYOUT_PARAMS)?.let {
                    mediaPlayerPresenter.rmpLayoutParams.postValue(it)
                }
            }

            InterprocessServiceBinder.LIVEDATA_RMP_MEDIA_URL -> {
                val mediaUrl = msg.data.getString(InterprocessServiceBinder.PARAM_RMP_MEDIA_URL)
                mediaPlayerPresenter.rmpMediaUrl.postValue(mediaUrl)
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