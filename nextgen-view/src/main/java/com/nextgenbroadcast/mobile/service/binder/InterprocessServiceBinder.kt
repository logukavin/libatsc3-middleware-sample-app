package com.nextgenbroadcast.mobile.service.binder

import android.net.Uri
import android.os.*
import androidx.core.os.bundleOf
import com.nextgenbroadcast.mobile.core.model.*
import com.nextgenbroadcast.mobile.core.presentation.*
import com.nextgenbroadcast.mobile.core.presentation.media.IObservablePlayer
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.service.handler.StandaloneClientHandler
import com.nextgenbroadcast.mobile.service.handler.OnIncomingPlayerStateListener
import kotlinx.coroutines.flow.MutableStateFlow

class InterprocessServiceBinder(
        service: IBinder
) : IServiceBinder {

    private var playerStateListener: IObservablePlayer.IPlayerStateListener? = null

    inner class SelectorPresenter : ISelectorPresenter {
        override val sltServices = MutableStateFlow<List<AVService>>(emptyList())
        override val selectedService = MutableStateFlow<AVService?>(null)

        override fun selectService(service: AVService): Boolean {
            sendAction(IServiceBinder.ACTION_SELECT_SERVICE, bundleOf(
                    IServiceBinder.PARAM_SELECT_SERVICE to service
            ))
            return true
        }
    }

    inner class ReceiverPresenter : IReceiverPresenter {
        override val receiverState = MutableStateFlow(ReceiverState.idle())
        override val freqKhz = MutableStateFlow(0)

        override fun openRoute(path: String): Boolean {
            sendAction(IServiceBinder.ACTION_OPEN_ROUTE, bundleOf(
                    IServiceBinder.PARAM_OPEN_ROUTE_PATH to path
            ))
            return true
        }

        override fun closeRoute() {
            sendAction(IServiceBinder.ACTION_CLOSE_ROUTE)
        }

        override fun tune(frequency: PhyFrequency) {
            sendAction(IServiceBinder.ACTION_TYNE_FREQUENCY, bundleOf(
                    IServiceBinder.PARAM_FREQUENCY to frequency
            ))
        }
    }

    inner class UserAgentPresenter : IUserAgentPresenter {
        override val appData = MutableStateFlow<AppData?>(null)
        override val appState = MutableStateFlow(ApplicationState.UNAVAILABLE)

        override fun setApplicationState(state: ApplicationState) {
            sendAction(IServiceBinder.ACTION_BA_STATE_CHANGED, bundleOf(
                    IServiceBinder.PARAM_APPSTATE to state
            ))
        }
    }

    inner class MediaPlayerPresenter : IMediaPlayerPresenter {
        override val rmpLayoutParams = MutableStateFlow(RPMParams())
        override val rmpMediaUri = MutableStateFlow<Uri?>(null)

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
        sendAction(IServiceBinder.TYPE_ALL)
    }

    fun close() {
        sendingMessenger = null
    }

    private fun sendAction(actionType: Int, args: Bundle? = null) {
        sendingMessenger?.send(Message.obtain(null, actionType).apply {
            data = args
            replyTo = incomingMessenger
        })
    }
}