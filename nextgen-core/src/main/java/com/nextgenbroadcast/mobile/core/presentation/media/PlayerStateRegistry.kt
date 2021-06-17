package com.nextgenbroadcast.mobile.core.presentation.media

import com.nextgenbroadcast.mobile.core.presentation.IMediaPlayerPresenter

class PlayerStateRegistry : CallbackRegistry<IObservablePlayer.IPlayerStateListener, IMediaPlayerPresenter, Any>(
        object : NotifierCallback<IObservablePlayer.IPlayerStateListener, IMediaPlayerPresenter?, Any>() {
            override fun onNotifyCallback(callback: IObservablePlayer.IPlayerStateListener, sender: IMediaPlayerPresenter?, notificationType: Int, arg2: Any?) {
                when (notificationType) {
                    STOP -> callback.onStop(sender)
                    PAUSE -> callback.onPause(sender)
                    RESUME -> callback.onResume(sender)
                }
            }
        }
) {

    fun notifyStop(playerPresenter: IMediaPlayerPresenter?) {
        notifyCallbacks(playerPresenter, STOP, null)
    }

    fun notifyPause(playerPresenter: IMediaPlayerPresenter?) {
        notifyCallbacks(playerPresenter, PAUSE, null)
    }

    fun notifyResume(playerPresenter: IMediaPlayerPresenter?) {
        notifyCallbacks(playerPresenter, RESUME, null)
    }

    companion object {
        private const val STOP = 0
        private const val PAUSE = 1
        private const val RESUME = 2
    }
}