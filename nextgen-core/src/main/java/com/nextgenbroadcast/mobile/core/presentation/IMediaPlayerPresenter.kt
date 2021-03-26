package com.nextgenbroadcast.mobile.core.presentation

import android.net.Uri
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.RPMParams
import com.nextgenbroadcast.mobile.core.presentation.media.IObservablePlayer
import kotlinx.coroutines.flow.StateFlow

interface IMediaPlayerPresenter: IObservablePlayer {
    val rmpLayoutParams: StateFlow<RPMParams>
    val rmpMediaUri: StateFlow<Uri?>

    fun rmpLayoutReset()
    fun rmpPlaybackChanged(state: PlaybackState)
    fun rmpPlaybackRateChanged(speed: Float)
    fun rmpMediaTimeChanged(currentTime: Long)
}