package org.ngbp.jsonrpc4jtestharness.controller

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.PlaybackState
import org.ngbp.jsonrpc4jtestharness.controller.model.RPMParams

interface IMediaPlayerController {
    val rpmParams: LiveData<RPMParams>

    fun rmpReset()
    fun rmpPlaybackChanged(state: PlaybackState)
}