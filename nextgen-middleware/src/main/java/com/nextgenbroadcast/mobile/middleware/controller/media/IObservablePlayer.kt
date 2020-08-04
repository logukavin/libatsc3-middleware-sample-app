package com.nextgenbroadcast.mobile.middleware.controller.media

import com.nextgenbroadcast.mobile.middleware.presentation.IMediaPlayerPresenter

interface IObservablePlayer {
    fun addOnPlayerSateChangedCallback(callback: IPlayerStateListener)

    fun removeOnPlayerSateChangedCallback(callback: IPlayerStateListener)

    interface IPlayerStateListener {
        fun onPause(mediaController: IMediaPlayerPresenter)
        fun onResume(mediaController: IMediaPlayerPresenter)
    }
}