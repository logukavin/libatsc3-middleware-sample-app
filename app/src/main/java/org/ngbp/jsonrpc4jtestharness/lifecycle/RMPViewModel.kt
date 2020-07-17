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
    val broadcastApplicationPlaybackState = Transformations.distinctUntilChanged(playerController.rmpPlayerState)

    val playerState = MutableLiveData<PlaybackState>()
    fun reset() {
        playerController.rmpReset()
    }

    fun setCurrentPlayerState(state: PlaybackState) {
        playerController.rmpPlaybackChanged(state)
    }

    fun pausePlayback() {
        playerState.postValue(PlaybackState.PAUSED)
    }

    fun restorePlayback() {
        playerState.postValue(PlaybackState.PLAYING)
    }
}

