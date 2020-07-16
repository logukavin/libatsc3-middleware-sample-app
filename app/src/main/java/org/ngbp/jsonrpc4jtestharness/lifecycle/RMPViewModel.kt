package org.ngbp.jsonrpc4jtestharness.lifecycle

import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.ngbp.jsonrpc4jtestharness.PlaybackState
import org.ngbp.jsonrpc4jtestharness.controller.IMediaPlayerController

class RMPViewModel(
        private val playerController: IMediaPlayerController
) : ViewModel() {
    val rmpParams = Transformations.distinctUntilChanged(playerController.rpmParams)

    fun reset() {
        playerController.rmpReset()
    }

    fun setState(state: PlaybackState) {
        playerController.rmpPlaybackChanged(state)
    }
}

