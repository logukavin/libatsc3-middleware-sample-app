package org.ngbp.jsonrpc4jtestharness.core.media

import org.ngbp.jsonrpc4jtestharness.presentation.IMediaPlayerPresenter

interface IObservablePlayer {
    fun addOnPlayerSateChangedCallback(callback: IPlayerStateListener)

    fun removeOnPlayerSateChangedCallback(callback: IPlayerStateListener)

    interface IPlayerStateListener {
        fun onPause(mediaController: IMediaPlayerPresenter)
        fun onResume(mediaController: IMediaPlayerPresenter)
    }
}