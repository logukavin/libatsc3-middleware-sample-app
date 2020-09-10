package com.nextgenbroadcast.mobile.middleware

import android.os.*
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.nextgenbroadcast.mobile.core.model.*
import com.nextgenbroadcast.mobile.middleware.controller.media.IObservablePlayer
import com.nextgenbroadcast.mobile.middleware.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IUserAgentPresenter

class InterprocessServiceBinder(
        private val service: IBinder
) : IServiceBinder {

    private var playerStateListener: IObservablePlayer.IPlayerStateListener? = null

    inner class SelectorPresenter : ISelectorPresenter {
        override val sltServices = MutableLiveData<List<SLSService>>()
        override val selectedService = MutableLiveData<SLSService?>()

        override fun selectService(service: SLSService) {
            sendAction(ACTION_SELECT_SERVICE, bundleOf(
                    PARAM_SELECT_SERVICE to service
            ))
        }
    }

    inner class ReceiverPresenter : IReceiverPresenter {
        override val receiverState = MutableLiveData<ReceiverState>()

        override fun openRoute(path: String): Boolean {
            sendAction(ACTION_OPEN_ROUTE, bundleOf(
                    PARAM_OPEN_ROUTE_PATH to path
            ))
            return true
        }

        override fun closeRoute() {
            sendAction(ACTION_CLOSE_ROUTE)

        }
    }

    inner class UserAgentPresenter : IUserAgentPresenter {
        override val appData = MutableLiveData<AppData?>()
    }

    inner class MediaPlayerPresenter : IMediaPlayerPresenter {
        override val rmpLayoutParams = MutableLiveData<RPMParams>()
        override val rmpMediaUrl = MutableLiveData<String?>()

        override fun rmpLayoutReset() {
            sendAction(ACTION_RMP_LAYOUT_RESET)
        }

        override fun rmpPlaybackChanged(state: PlaybackState) {
            sendAction(ACTION_RMP_PLAYBACK_STATE_CHANGED, bundleOf(
                    PARAM_RMP_PLAYBACK_STATE to state
            ))
        }

        override fun rmpPlaybackRateChanged(speed: Float) {
            sendAction(ACTION_RMP_PLAYBACK_RATE_CHANGED, bundleOf(
                    PARAM_RMP_PLAYBACK_RATE to speed
            ))
        }

        override fun rmpMediaTimeChanged(currentTime: Long) {
            sendAction(ACTION_RMP_MEDIA_TIME_CHANGED, bundleOf(
                    PARAM_RMP_MEDIA_TIME to currentTime
            ))
        }

        override fun addOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener) {
            playerStateListener = callback
            sendAction(CALLBACK_ADD_PLAYER_STATE_CHANGE)
        }

        override fun removeOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener) {
            playerStateListener = null
            sendAction(CALLBACK_REMOVE_PLAYER_STATE_CHANGE)
        }
    }

    override val selectorPresenter = SelectorPresenter()

    override val receiverPresenter = ReceiverPresenter()

    override val userAgentPresenter = UserAgentPresenter()

    override val mediaPlayerPresenter = MediaPlayerPresenter()

    private val sendingMessenger = Messenger(service)

    private val incomingMessenger = Messenger(Atsc3ActivityIncomingHandler(object : OnIncomingDataListener {

        override fun onReceiverState(receiverState: ReceiverState) {
            receiverPresenter.receiverState.postValue(receiverState)
        }

        override fun onSLSServices(slsServices: List<SLSService>) {
            selectorPresenter.sltServices.postValue(slsServices)
        }

        override fun onSelectSLSService(slsService: SLSService?) {
            selectorPresenter.selectedService.postValue(slsService)
        }

        override fun onAppData(appData: AppData?) {
            userAgentPresenter.appData.postValue(appData)
        }

        override fun onRPMParams(ppmParams: RPMParams) {
            mediaPlayerPresenter.rmpLayoutParams.postValue(ppmParams)
        }

        override fun onRPMMediaUrl(rpmMediaUrl: String) {
            mediaPlayerPresenter.rmpMediaUrl.postValue(rpmMediaUrl)
        }

        override fun onPlayerStatePause() {
            playerStateListener?.onPause(mediaPlayerPresenter)
        }

        override fun onPlayerStateResume() {
            playerStateListener?.onResume(mediaPlayerPresenter)
        }

    }))

    init {
        listOf(
                LIVEDATA_RECEIVER_STATE,
                LIVEDATA_SERVICE_LIST,
                LIVEDATA_SERVICE_SELECTED,
                LIVEDATA_APPDATA,
                LIVEDATA_RMP_LAYOUT_PARAMS,
                LIVEDATA_RMP_MEDIA_URL).forEach {
            subscribe(it)
        }
    }

    private fun subscribe(dataType: Int) {
        sendingMessenger.send(Message.obtain(null, dataType).apply {
            replyTo = incomingMessenger
        })
    }

    private fun sendAction(actionType: Int, args: Bundle? = null) {
        sendingMessenger.send(Message.obtain(null, actionType).apply {
            data = args
            replyTo = incomingMessenger
        })
    }

    companion object {
        const val LIVEDATA_RECEIVER_STATE = 1
        const val LIVEDATA_SERVICE_LIST = 2
        const val LIVEDATA_SERVICE_SELECTED = 3
        const val LIVEDATA_APPDATA = 4
        const val LIVEDATA_RMP_LAYOUT_PARAMS = 5
        const val LIVEDATA_RMP_MEDIA_URL = 6

        const val ACTION_OPEN_ROUTE = 7
        const val ACTION_CLOSE_ROUTE = 8
        const val ACTION_SELECT_SERVICE = 9
        const val ACTION_RMP_LAYOUT_RESET = 10
        const val ACTION_RMP_PLAYBACK_STATE_CHANGED = 11
        const val ACTION_RMP_PLAYBACK_RATE_CHANGED = 12
        const val ACTION_RMP_MEDIA_TIME_CHANGED = 13

        const val CALLBACK_ADD_PLAYER_STATE_CHANGE = 14
        const val CALLBACK_REMOVE_PLAYER_STATE_CHANGE = 15

        const val ACTION_PLAYER_STATE_CHANGE_PAUSE = 16
        const val ACTION_PLAYER_STATE_CHANGE_RESUME = 17

        const val PARAM_RECEIVER_STATE = "PARAM_RECEIVER_STATE"
        const val PARAM_SERVICE_LIST = "PARAM_SERVICE_LIST"
        const val PARAM_SERVICE_SELECTED = "PARAM_SERVICE_SELECTED"
        const val PARAM_APPDATA = "PARAM_APPDATA"
        const val PARAM_RMP_LAYOUT_PARAMS = "PARAM_RMP_LAYOUT_PARAMS"
        const val PARAM_RMP_MEDIA_URL = "PARAM_RMP_MEDIA_URL"

        const val PARAM_OPEN_ROUTE_PATH = "PARAM_OPEN_ROUTE_PATH"
        const val PARAM_SELECT_SERVICE = "PARAM_SELECT_SERVICE"
        const val PARAM_RMP_PLAYBACK_STATE = "PARAM_RMP_PLAYBACK_STATE"
        const val PARAM_RMP_PLAYBACK_RATE = "PARAM_RMP_PLAYBACK_RATE"
        const val PARAM_RMP_MEDIA_TIME = "PARAM_RMP_MEDIA_TIME"
    }
}