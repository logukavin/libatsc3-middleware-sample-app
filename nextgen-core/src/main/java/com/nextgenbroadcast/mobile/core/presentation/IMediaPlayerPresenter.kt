package com.nextgenbroadcast.mobile.core.presentation

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.RPMParams
import com.nextgenbroadcast.mobile.core.presentation.media.IObservablePlayer

interface IMediaPlayerPresenter: IObservablePlayer {
    val rmpLayoutParams: LiveData<RPMParams>
    val rmpMediaUrl: LiveData<String?>

    fun rmpLayoutReset()
    fun rmpPlaybackChanged(state: PlaybackState)
    fun rmpPlaybackRateChanged(speed: Float)
    fun rmpMediaTimeChanged(currentTime: Long)
}