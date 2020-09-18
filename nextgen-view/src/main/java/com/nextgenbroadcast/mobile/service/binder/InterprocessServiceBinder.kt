package com.nextgenbroadcast.mobile.service.binder

import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nextgenbroadcast.mobile.core.model.*
import com.nextgenbroadcast.mobile.core.presentation.*
import com.nextgenbroadcast.mobile.core.presentation.media.IObservablePlayer
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.service.handler.StandaloneClientHandler
import com.nextgenbroadcast.mobile.service.handler.OnIncomingPlayerStateListener
import java.lang.UnsupportedOperationException

class InterprocessServiceBinder(
        service: IBinder
) : IServiceBinder {

    private var playerStateListener: IObservablePlayer.IPlayerStateListener? = null

    inner class SelectorPresenter : ISelectorPresenter {
        override val sltServices = MutableLiveData<List<SLSService>>()
        override val selectedService = MutableLiveData<SLSService?>()

        override fun selectService(service: SLSService) {
            sendAction(IServiceBinder.ACTION_SELECT_SERVICE, bundleOf(
                    IServiceBinder.PARAM_SELECT_SERVICE to service
            ))
        }
    }

    inner class ReceiverPresenter : IReceiverPresenter {
        override val receiverState = MutableLiveData<ReceiverState>()

        override fun openRoute(path: String): Boolean {
            sendAction(IServiceBinder.ACTION_OPEN_ROUTE, bundleOf(
                    IServiceBinder.PARAM_OPEN_ROUTE_PATH to path
            ))
            return true
        }

        override fun closeRoute() {
            sendAction(IServiceBinder.ACTION_CLOSE_ROUTE)
        }

        override fun createMMTSource(): Any {
            throw UnsupportedOperationException("MMT playback is not supported with standalone service")
        }
    }

    inner class UserAgentPresenter : IUserAgentPresenter {
        override val appData = MutableLiveData<AppData?>()
    }

    inner class MediaPlayerPresenter : IMediaPlayerPresenter {
        override val rmpLayoutParams = MutableLiveData<RPMParams>()
        override val rmpMediaUrl = MutableLiveData<String?>()
        override val rmpMediaUri = MutableLiveData<Uri?>()

        private var callback: UriPermissionsObtainedListener? = null

        override fun rmpLayoutReset() {
            sendAction(IServiceBinder.ACTION_RMP_LAYOUT_RESET)
        }

        override fun rmpPlaybackChanged(state: PlaybackState) {
            sendAction(IServiceBinder.ACTION_RMP_PLAYBACK_STATE_CHANGED, bundleOf(
                    IServiceBinder.PARAM_RMP_PLAYBACK_STATE to state
            ))
        }

        override fun rmpPlaybackRateChanged(speed: Float) {
            sendAction(IServiceBinder.ACTION_RMP_PLAYBACK_RATE_CHANGED, bundleOf(
                    IServiceBinder.PARAM_RMP_PLAYBACK_RATE to speed
            ))
        }

        override fun rmpMediaTimeChanged(currentTime: Long) {
            sendAction(IServiceBinder.ACTION_RMP_MEDIA_TIME_CHANGED, bundleOf(
                    IServiceBinder.PARAM_RMP_MEDIA_TIME to currentTime
            ))
        }

        override fun addOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener) {
            playerStateListener = callback
            sendAction(IServiceBinder.CALLBACK_ADD_PLAYER_STATE_CHANGE)
        }

        override fun removeOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener) {
            playerStateListener = null
            sendAction(IServiceBinder.CALLBACK_REMOVE_PLAYER_STATE_CHANGE)
        }

        override fun needPermissions(uri: Uri, callback: UriPermissionsObtainedListener) {
            sendAction(IServiceBinder.ACTION_NEED_URI_PERMISSION, bundleOf(
                    IServiceBinder.PARAM_URI_NEED_PERMISSION to uri
            ))
            this.callback = callback
            Log.d("TEST", "send uri need permissions $uri")
        }

        override fun havPermissions() {
            callback?.onPermissionsObtained()
        }
    }

    override val selectorPresenter = SelectorPresenter()

    override val receiverPresenter = ReceiverPresenter()

    override val userAgentPresenter = UserAgentPresenter()

    override val mediaPlayerPresenter = MediaPlayerPresenter()

    private var sendingMessenger: Messenger? = Messenger(service)

    private val incomingMessenger = Messenger(StandaloneClientHandler(
            selectorPresenter,
            receiverPresenter,
            userAgentPresenter,
            mediaPlayerPresenter,
            object : OnIncomingPlayerStateListener {
                override fun onPlayerStatePause() {
                    playerStateListener?.onPause(mediaPlayerPresenter)
                }

                override fun onPlayerStateResume() {
                    playerStateListener?.onResume(mediaPlayerPresenter)
                }
            }
    ))

    init {
        subscribe(IServiceBinder.LIVEDATA_ALL)
    }

    fun close() {
        sendingMessenger = null;
    }

    private fun subscribe(dataType: Int) {
        sendingMessenger?.send(Message.obtain(null, dataType).apply {
            replyTo = incomingMessenger
        })
    }

    private fun sendAction(actionType: Int, args: Bundle? = null) {
        sendingMessenger?.send(Message.obtain(null, actionType).apply {
            data = args
            replyTo = incomingMessenger
        })
    }

}