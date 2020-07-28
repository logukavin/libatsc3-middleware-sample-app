package org.ngbp.jsonrpc4jtestharness.controller.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import org.ngbp.jsonrpc4jtestharness.core.media.IObservablePlayer
import org.ngbp.jsonrpc4jtestharness.core.media.PlayerStateRegistry
import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.core.model.RPMParams
import org.ngbp.jsonrpc4jtestharness.core.repository.IRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViewControllerImpl @Inject constructor(
        private val repository: IRepository
) : IViewController {

    private enum class PlaybackSource {
        BROADCAST, BROADBAND
    }

    private val playbackSource = MutableLiveData<PlaybackSource>(PlaybackSource.BROADCAST)
    private val externalMediaUrl = MutableLiveData<String>()
    private val rmpListeners = PlayerStateRegistry()
    private val selectedService = Transformations.distinctUntilChanged(repository.selectedService)

    override val appData = repository.appData

    override val rmpLayoutParams = MutableLiveData<RPMParams>(RPMParams())
    override val rmpMediaUrl = Transformations.switchMap(playbackSource) { source ->
        if (source == PlaybackSource.BROADCAST) {
            repository.routeMediaUrl
        } else {
            externalMediaUrl
        }
    }

    override val rmpState = MutableLiveData<PlaybackState>(PlaybackState.IDLE)
    override val rmpMediaTime = MutableLiveData<Double>()
    override val rmpPlaybackRate = MutableLiveData<Float>()

    init {
        selectedService.observeForever {
            rmpReset()
        }
    }

    override fun rmpReset() {
        rmpLayoutParams.postValue(RPMParams())
    }

    override fun rmpPlaybackChanged(state: PlaybackState) {
        rmpState.postValue(state)
    }

    override fun rmpPause() {
        rmpListeners.notifyPause(this)
    }

    override fun rmpResume() {
        rmpListeners.notifyResume(this)
    }

    override fun addOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener) {
        rmpListeners.add(callback)
    }

    override fun removeOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener) {
        rmpListeners.remove(callback)
    }

    override fun updateRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double) {
        rmpLayoutParams.postValue(RPMParams(scaleFactor, xPos.toInt(), yPos.toInt()))
    }

    override fun rmpPlaybackRateChanged(speed: Float) {
        rmpPlaybackRate.postValue(speed)
    }

    //TODO: currently delay not supported and blocked on RPC level
    override fun requestMediaPlay(mediaUrl: String?, delay: Long) {
        rmpPause()
        if (mediaUrl != null) {
            playbackSource.value = PlaybackSource.BROADBAND
        } else {
            playbackSource.value = PlaybackSource.BROADCAST
        }
        externalMediaUrl.value = mediaUrl
        rmpResume()
    }

    //TODO: currently delay not supported and blocked on RPC level
    override fun requestMediaStop(delay: Long) {
        rmpPause()
    }
}