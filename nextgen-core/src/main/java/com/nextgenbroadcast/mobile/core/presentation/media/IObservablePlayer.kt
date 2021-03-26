package com.nextgenbroadcast.mobile.core.presentation.media

import com.nextgenbroadcast.mobile.core.presentation.IMediaPlayerPresenter

interface IObservablePlayer {
    fun addOnPlayerSateChangedCallback(callback: IPlayerStateListener)

    fun removeOnPlayerSateChangedCallback(callback: IPlayerStateListener)

    interface IPlayerStateListener {
        fun onPause(mediaController: IMediaPlayerPresenter?)
        fun onResume(mediaController: IMediaPlayerPresenter?)
    }
}