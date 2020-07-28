package org.ngbp.jsonrpc4jtestharness.presentation

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.core.media.IObservablePlayer
import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.core.model.RPMParams

interface IMediaPlayerPresenter: IObservablePlayer {
    val rmpLayoutParams: LiveData<RPMParams>
    val rmpMediaUrl: LiveData<String?>

    fun rmpReset()
    fun rmpPlaybackChanged(state: PlaybackState)
    fun rmpPlaybackRateChanged(speed: Float)
}