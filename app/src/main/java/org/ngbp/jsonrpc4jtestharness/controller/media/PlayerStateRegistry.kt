package org.ngbp.jsonrpc4jtestharness.controller.media

import androidx.databinding.CallbackRegistry
import org.ngbp.jsonrpc4jtestharness.controller.IMediaPlayerController

class PlayerStateRegistry : CallbackRegistry<IObservablePlayer.IPlayerStateListener, IMediaPlayerController, Any>(
        object : NotifierCallback<IObservablePlayer.IPlayerStateListener, IMediaPlayerController, Any>() {
            override fun onNotifyCallback(callback: IObservablePlayer.IPlayerStateListener, sender: IMediaPlayerController, notificationType: Int, arg2: Any?) {
                when (notificationType) {
                    PAUSE -> callback.onPause(sender)
                    RESUME -> callback.onResume(sender)
                }
            }
        }
) {

    fun notifyPause(mediaController: IMediaPlayerController) {
        notifyCallbacks(mediaController, PAUSE, null)
    }

    fun notifyResume(mediaController: IMediaPlayerController) {
        notifyCallbacks(mediaController, RESUME, null)
    }

    companion object {
        private const val PAUSE = 1
        private const val RESUME = 2
    }
}