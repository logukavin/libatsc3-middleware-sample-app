package org.ngbp.jsonrpc4jtestharness.lifecycle

import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.ngbp.jsonrpc4jtestharness.PlaybackState
import org.ngbp.jsonrpc4jtestharness.controller.IReceiverController

class RMPViewModel(
        private val controller: IReceiverController
) : ViewModel() {
    val rmpParams = Transformations.distinctUntilChanged(controller.rpmParams)

    fun reset() {
        controller.resetRMP()
    }

    fun setState(state: PlaybackState) {
        controller.rmpPlaybackChanged(state.state)
    }
}

