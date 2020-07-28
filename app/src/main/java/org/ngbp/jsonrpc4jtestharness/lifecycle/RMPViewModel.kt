package org.ngbp.jsonrpc4jtestharness.lifecycle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.ngbp.jsonrpc4jtestharness.core.media.IObservablePlayer
import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.presentation.IMediaPlayerPresenter

class RMPViewModel(
        private val presenter: IMediaPlayerPresenter
) : ViewModel(), IObservablePlayer.IPlayerStateListener {
    private val _playWhenReady = MutableLiveData<Boolean>(true)

    val layoutParams = Transformations.distinctUntilChanged(presenter.rmpLayoutParams)
    val mediaUri = Transformations.distinctUntilChanged(presenter.rmpMediaUrl)

    val playWhenReady: LiveData<Boolean> = _playWhenReady

    init {
        presenter.addOnPlayerSateChangedCallback(this)
    }

    override fun onCleared() {
        super.onCleared()

        presenter.removeOnPlayerSateChangedCallback(this)
    }

    override fun onPause(mediaController: IMediaPlayerPresenter) {
        _playWhenReady.value = false
    }

    override fun onResume(mediaController: IMediaPlayerPresenter) {
        _playWhenReady.value = true
    }

    fun reset() {
        presenter.rmpReset()
        _playWhenReady.value = true
    }

    fun setCurrentPlayerState(state: PlaybackState) {
        presenter.rmpPlaybackChanged(state)
    }

    fun setCurrentPlaybackRate(speed: Float) {
        presenter.rmpPlaybackRateChanged(speed = speed)
    }
}

