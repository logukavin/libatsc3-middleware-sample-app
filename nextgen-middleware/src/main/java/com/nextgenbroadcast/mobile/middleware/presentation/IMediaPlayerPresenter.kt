package com.nextgenbroadcast.mobile.middleware.presentation

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.RPMParams
import com.nextgenbroadcast.mobile.middleware.controller.media.IObservablePlayer

interface IMediaPlayerPresenter: IObservablePlayer {
    val rmpLayoutParams: LiveData<RPMParams>
    val rmpMediaUrl: LiveData<String?>

    fun rmpReset()
    fun rmpPlaybackChanged(state: PlaybackState)
    fun rmpPlaybackRateChanged(speed: Float)
    fun rmpMediaTimeChanged(currentTime: Long)
}