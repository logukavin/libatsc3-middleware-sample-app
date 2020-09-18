package com.nextgenbroadcast.mobile.core.presentation

import android.net.Uri
import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.RPMParams
import com.nextgenbroadcast.mobile.core.presentation.media.IObservablePlayer

interface IMediaPlayerPresenter: IObservablePlayer {
    val rmpLayoutParams: LiveData<RPMParams>
    val rmpMediaUrl: LiveData<String?>
    val rmpMediaUri: LiveData<Uri?>

    fun rmpLayoutReset()
    fun rmpPlaybackChanged(state: PlaybackState)
    fun rmpPlaybackRateChanged(speed: Float)
    fun rmpMediaTimeChanged(currentTime: Long)
}