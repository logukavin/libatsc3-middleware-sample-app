package org.ngbp.jsonrpc4jtestharness.lifecycle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.controller.IMediaPlayerController

class RMPViewModel(
        private val playerController: IMediaPlayerController
) : ViewModel() {
    val layoutParams = Transformations.distinctUntilChanged(playerController.rmpParams)
    val mediaUri = Transformations.distinctUntilChanged(playerController.rmpMediaUrl)
    val playerState = Transformations.distinctUntilChanged(playerController.rmpPlayerState)

    fun reset() {
        playerController.rmpReset()
    }

    fun setCurrentPlayerState(state: PlaybackState) {
        playerController.rmpPlaybackChanged(state)
    }

    fun pausePlayback() {
        playerController.rmpPlayerState.postValue(PlaybackState.PAUSED)
    }

    fun restorePlayback() {
        playerController.rmpPlayerState.postValue(PlaybackState.PLAYING)
    }
}

