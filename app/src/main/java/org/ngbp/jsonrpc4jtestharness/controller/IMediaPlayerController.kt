package org.ngbp.jsonrpc4jtestharness.controller

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.controller.media.IObservablePlayer
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.controller.model.RPMParams

interface IMediaPlayerController: IObservablePlayer {
    val rmpParams: LiveData<RPMParams>
    val rmpMediaUrl: LiveData<String?>
    val rmpState: LiveData<PlaybackState>

    fun rmpReset()
    fun rmpPlaybackChanged(state: PlaybackState)

    fun rmpPause()
    fun rmpResume()
}