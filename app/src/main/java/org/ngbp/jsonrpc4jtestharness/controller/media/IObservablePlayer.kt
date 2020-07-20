package org.ngbp.jsonrpc4jtestharness.controller.media

import org.ngbp.jsonrpc4jtestharness.controller.IMediaPlayerController

interface IObservablePlayer {
    fun addOnPlayerSateChangedCallback(callback: IPlayerStateListener)

    fun removeOnPlayerSateChangedCallback(callback: IPlayerStateListener)

    interface IPlayerStateListener {
        fun onPause(mediaController: IMediaPlayerController)
        fun onResume(mediaController: IMediaPlayerController)
    }
}