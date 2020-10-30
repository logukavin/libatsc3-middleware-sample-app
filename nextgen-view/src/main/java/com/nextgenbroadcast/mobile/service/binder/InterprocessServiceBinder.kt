package com.nextgenbroadcast.mobile.service.binder

import android.net.Uri
import android.os.*
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.nextgenbroadcast.mobile.permission.UriPermissionProvider
import com.nextgenbroadcast.mobile.core.model.*
import com.nextgenbroadcast.mobile.core.presentation.*
import com.nextgenbroadcast.mobile.core.presentation.media.IObservablePlayer
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.service.handler.StandaloneClientHandler
import com.nextgenbroadcast.mobile.service.handler.OnIncomingPlayerStateListener
import com.nextgenbroadcast.mobile.permission.IUriPermissionRequester
import java.lang.UnsupportedOperationException

class InterprocessServiceBinder(
        service: IBinder,
        private val clientPackage: String,
        uriPermissionProvider: UriPermissionProvider? = null
) : IServiceBinder, IUriPermissionRequester {

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
        override val freqKhz = MutableLiveData<Int>()

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

        override fun tune(freqKhz: Int) {
            sendAction(IServiceBinder.ACTION_TYNE_FREQUENCY, bundleOf(
                    IServiceBinder.PARAM_FREQUENCY_KHZ to freqKhz
            ))
        }
    }

    inner class UserAgentPresenter : IUserAgentPresenter {
        override val appData = MutableLiveData<AppData?>()
        override val appState = MutableLiveData<Int?>()

        override fun setState(state: Int) {
            sendAction(IServiceBinder.ACTION_BA_STATE_CHANGED, bundleOf(
                    IServiceBinder.PARAM_APPSTATE to state
            ))
        }
    }

    inner class MediaPlayerPresenter : IMediaPlayerPresenter {
        override val rmpLayoutParams = MutableLiveData<RPMParams>()
        override val rmpMediaUri = MutableLiveData<Uri?>()

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
            uriPermissionProvider,
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
        sendAction(IServiceBinder.LIVEDATA_ALL, bundleOf(
                IServiceBinder.PARAM_PERMISSION_PACKAGE to clientPackage
        ))
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

    override fun requestUriPermission(uri: Uri, clientPackage: String) {
        sendAction(IServiceBinder.ACTION_NEED_URI_PERMISSION, bundleOf(
                IServiceBinder.PARAM_URI_NEED_PERMISSION to uri,
                IServiceBinder.PARAM_PERMISSION_PACKAGE to clientPackage
        ))
    }
}