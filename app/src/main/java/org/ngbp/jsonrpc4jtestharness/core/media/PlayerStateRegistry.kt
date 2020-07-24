package org.ngbp.jsonrpc4jtestharness.core.media

import androidx.databinding.CallbackRegistry
import org.ngbp.jsonrpc4jtestharness.presentation.IMediaPlayerPresenter

class PlayerStateRegistry : CallbackRegistry<IObservablePlayer.IPlayerStateListener, IMediaPlayerPresenter, Any>(
        object : NotifierCallback<IObservablePlayer.IPlayerStateListener, IMediaPlayerPresenter, Any>() {
            override fun onNotifyCallback(callback: IObservablePlayer.IPlayerStateListener, sender: IMediaPlayerPresenter, notificationType: Int, arg2: Any?) {
                when (notificationType) {
                    PAUSE -> callback.onPause(sender)
                    RESUME -> callback.onResume(sender)
                }
            }
        }
) {

    fun notifyPause(playerPresenter: IMediaPlayerPresenter) {
        notifyCallbacks(playerPresenter, PAUSE, null)
    }

    fun notifyResume(playerPresenter: IMediaPlayerPresenter) {
        notifyCallbacks(playerPresenter, RESUME, null)
    }

    companion object {
        private const val PAUSE = 1
        private const val RESUME = 2
    }
}