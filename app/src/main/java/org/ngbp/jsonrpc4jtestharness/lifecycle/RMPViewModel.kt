package org.ngbp.jsonrpc4jtestharness.lifecycle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.controller.IMediaPlayerController
import org.ngbp.jsonrpc4jtestharness.controller.media.IObservablePlayer

class RMPViewModel(
        private val playerController: IMediaPlayerController
) : ViewModel(), IObservablePlayer.IPlayerStateListener {
    private val _playWhenReady = MutableLiveData<Boolean>(true)

    val layoutParams = Transformations.distinctUntilChanged(playerController.rmpParams)
    val mediaUri = Transformations.distinctUntilChanged(playerController.rmpMediaUrl)

    val playWhenReady: LiveData<Boolean> = _playWhenReady

    init {
        playerController.addOnPlayerSateChangedCallback(this)
    }

    override fun onCleared() {
        super.onCleared()

        playerController.removeOnPlayerSateChangedCallback(this)
    }

    override fun onPause(mediaController: IMediaPlayerController) {
        _playWhenReady.value = false
    }

    override fun onResume(mediaController: IMediaPlayerController) {
        _playWhenReady.value = true
    }

    fun reset() {
        playerController.rmpReset()
        _playWhenReady.value = true
    }

    fun setCurrentPlayerState(state: PlaybackState) {
        playerController.rmpPlaybackChanged(state)
    }
}

