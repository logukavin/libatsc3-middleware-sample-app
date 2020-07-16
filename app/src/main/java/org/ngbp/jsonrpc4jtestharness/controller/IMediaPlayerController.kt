package org.ngbp.jsonrpc4jtestharness.controller

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.controller.model.RPMParams

interface IMediaPlayerController {
    val rmpParams: LiveData<RPMParams>
    val rmpMediaUrl: LiveData<String?>

    fun rmpReset()
    fun rmpPlaybackChanged(state: PlaybackState)
}