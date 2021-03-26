package com.nextgenbroadcast.mobile.service.handler

import android.net.Uri
import android.os.Handler
import android.os.Message
import com.nextgenbroadcast.mobile.core.getParcelable
import com.nextgenbroadcast.mobile.core.getParcelableArrayList
import com.nextgenbroadcast.mobile.core.model.*
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.service.binder.InterprocessServiceBinder

internal class StandaloneClientHandler(
        private val selectorPresenter: InterprocessServiceBinder.SelectorPresenter,
        private val receiverPresenter: InterprocessServiceBinder.ReceiverPresenter,
        private val userAgentPresenter: InterprocessServiceBinder.UserAgentPresenter,
        private val mediaPlayerPresenter: InterprocessServiceBinder.MediaPlayerPresenter,
        private val incomingDataListener: OnIncomingPlayerStateListener
) : Handler() {

    override fun handleMessage(msg: Message) {
        when (msg.what) {

            IServiceBinder.TYPE_RECEIVER_STATE -> {
                msg.data.getParcelable(ReceiverState::class.java, IServiceBinder.PARAM_RECEIVER_STATE)?.let {
                    receiverPresenter.receiverState.value = it
                }
            }

            IServiceBinder.TYPE_SERVICE_LIST -> {
                msg.data.getParcelableArrayList(AVService::class.java, IServiceBinder.PARAM_SERVICE_LIST)?.let {
                    selectorPresenter.sltServices.value = it
                }
            }

            IServiceBinder.TYPE_SERVICE_SELECTED -> {
                val selectedService = msg.data.getParcelable(AVService::class.java, IServiceBinder.PARAM_SERVICE_SELECTED)
                selectorPresenter.selectedService.value = selectedService
            }

            IServiceBinder.TYPE_APPDATA -> {
                val appData = msg.data.getParcelable(AppData::class.java, IServiceBinder.PARAM_APPDATA)
                userAgentPresenter.appData.value = appData
            }

            IServiceBinder.TYPE_RMP_LAYOUT_PARAMS -> {
                msg.data.getParcelable(RPMParams::class.java, IServiceBinder.PARAM_RMP_LAYOUT_PARAMS)?.let {
                    mediaPlayerPresenter.rmpLayoutParams.value = (it)
                }
            }

            IServiceBinder.TYPE_RMP_MEDIA_URI -> {
                val uri = msg.data.getParcelable(Uri::class.java, IServiceBinder.PARAM_RMP_MEDIA_URI)
                mediaPlayerPresenter.rmpMediaUri.value = uri
            }

            IServiceBinder.ACTION_PLAYER_STATE_CHANGE_PAUSE -> {
                incomingDataListener.onPlayerStatePause()
            }

            IServiceBinder.ACTION_PLAYER_STATE_CHANGE_RESUME -> {
                incomingDataListener.onPlayerStateResume()
            }

            IServiceBinder.ACTION_TYNE_FREQUENCY -> {
                receiverPresenter.freqKhz.value = msg.data.getInt(IServiceBinder.PARAM_FREQUENCY_KHZ)
            }
        }
    }

}