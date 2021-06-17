package com.nextgenbroadcast.mobile.middleware.sample.lifecycle

import android.net.Uri
import androidx.lifecycle.*
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.RPMParams
import com.nextgenbroadcast.mobile.core.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.core.presentation.media.IObservablePlayer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

//TODO: replace with ContentResolver
class RMPViewModel(
        private val presenter: IMediaPlayerPresenter
) : ViewModel(), IObservablePlayer.IPlayerStateListener {
    private val _requestedState = MutableLiveData(PlaybackState.PLAYING)
    private val _mediaUri = MutableLiveData<Uri?>()
    private val _layoutParams = MutableLiveData<RPMParams>()

    val layoutParams: LiveData<RPMParams> = _layoutParams
    val mediaUri: LiveData<Uri?> = _mediaUri
    val requestedState: LiveData<PlaybackState> = _requestedState

    var rmpState = PlaybackState.IDLE
        private set

    init {
        presenter.addOnPlayerSateChangedCallback(this)
        // we must collect data in viewModelScope instead of asLiveData() because VM life cycle is different from Fragment ones
        viewModelScope.launch {
            presenter.rmpMediaUri.collect {
                _mediaUri.value = it
            }
        }
        viewModelScope.launch {
            presenter.rmpLayoutParams.collect {
                _layoutParams.value = it
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        presenter.removeOnPlayerSateChangedCallback(this)
    }

    override fun onStop(mediaController: IMediaPlayerPresenter?) {
        _requestedState.value = PlaybackState.IDLE
    }

    override fun onPause(mediaController: IMediaPlayerPresenter?) {
        _requestedState.value = PlaybackState.PAUSED
    }

    override fun onResume(mediaController: IMediaPlayerPresenter?) {
        _requestedState.value = PlaybackState.PLAYING
    }

    fun reset() {
        presenter.rmpLayoutReset()
        _requestedState.value = PlaybackState.PLAYING
    }

    fun setCurrentPlayerState(state: PlaybackState) {
        rmpState = state
        presenter.rmpPlaybackChanged(state)
    }

    fun setCurrentPlaybackRate(speed: Float) {
        presenter.rmpPlaybackRateChanged(speed)
    }

    fun setCurrentMediaTime(currentTime: Long) {
        presenter.rmpMediaTimeChanged(currentTime)
    }
}

