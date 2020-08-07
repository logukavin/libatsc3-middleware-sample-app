package com.nextgenbroadcast.mobile.middleware.controller.view

import androidx.lifecycle.*
import com.nextgenbroadcast.mobile.core.mapWith
import com.nextgenbroadcast.mobile.core.md5
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.RPMParams
import com.nextgenbroadcast.mobile.middleware.controller.media.IObservablePlayer
import com.nextgenbroadcast.mobile.middleware.controller.media.PlayerStateRegistry
import com.nextgenbroadcast.mobile.middleware.repository.IRepository

internal class ViewControllerImpl (
        private val repository: IRepository
) : IViewController {

    private enum class PlaybackSource {
        BROADCAST, BROADBAND
    }

    private val playbackSource = MutableLiveData<PlaybackSource>(PlaybackSource.BROADCAST)
    private val externalMediaUrl = MutableLiveData<String>()
    private val rmpListeners = PlayerStateRegistry()

    override val appData: LiveData<AppData?> = repository.heldPackage.mapWith(repository.applications) { (held, applications) ->
        held?.let {
            val appContextId = held.appContextId ?: return@let null
            val hostName = repository.hostName
            val httpPost = repository.httpsPort
            val socketPort = repository.wsPort
            val rev = 20180720 //TODO: use real revision
            val appEntryPage = held.bcastEntryPageUrl?.let { entryPage ->
                val contextPath = appContextId.md5()
                "https://$hostName:$httpPost/$contextPath/$entryPage"
            } ?: held.bbandEntryPageUrl ?: return@let null
            val compatibleServiceIds = held.coupledServices ?: emptyList()
            val application = applications?.firstOrNull { app ->
                app.appContextIdList.contains(appContextId)
            }
            val appUrl = "$appEntryPage?wsURL=ws://$hostName:$socketPort&rev=$rev"

            AppData(appContextId, appUrl, compatibleServiceIds, application?.cachePath)
        }
    }

    override val rmpLayoutParams = MutableLiveData<RPMParams>(RPMParams())
    override val rmpMediaUrl = Transformations.switchMap(playbackSource) { source ->
        if (source == PlaybackSource.BROADCAST) {
            repository.routeMediaUrl
        } else {
            externalMediaUrl
        }
    }

    override val rmpState = MutableLiveData<PlaybackState>(PlaybackState.IDLE)
    override val rmpMediaTime = MutableLiveData<Long>()
    override val rmpPlaybackRate = MutableLiveData<Float>()

    init {
        repository.selectedService.distinctUntilChanged().observeForever {
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

    override fun rmpMediaTimeChanged(currentTime: Long) {
        rmpMediaTime.postValue(currentTime)
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