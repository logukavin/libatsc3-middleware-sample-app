package com.nextgenbroadcast.mobile.middleware.controller.view

import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.map
import com.nextgenbroadcast.mobile.core.mapWith
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.RPMParams
import com.nextgenbroadcast.mobile.core.presentation.ApplicationState
import com.nextgenbroadcast.mobile.core.presentation.media.IObservablePlayer
import com.nextgenbroadcast.mobile.core.presentation.media.PlayerStateRegistry
import com.nextgenbroadcast.mobile.middleware.analytics.IAtsc3Analytics
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.SLTConstants
import com.nextgenbroadcast.mobile.middleware.service.provider.IMediaFileProvider
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.server.ServerUtils
import com.nextgenbroadcast.mobile.middleware.settings.IClientSettings

internal class ViewControllerImpl(
        private val repository: IRepository,
        private val settings: IClientSettings,
        private val fileProvider: IMediaFileProvider,
        private val atsc3Analytics: IAtsc3Analytics,
        private val ignoreAudioServiceMedia: Boolean,
        private val rmpListeners: PlayerStateRegistry = PlayerStateRegistry()
) : IViewController {

    private enum class PlaybackSource {
        BROADCAST, BROADBAND
    }

    private val playbackSource = MutableLiveData(PlaybackSource.BROADCAST)
    private val externalMediaUrl = MutableLiveData<String?>()

    override val appData: LiveData<AppData?> = repository.heldPackage.mapWith(repository.applications) { (held, applications) ->
        held?.let {
            val appContextId = held.appContextId ?: return@let null
            val appUrl = held.bcastEntryPageUrl?.let { entryPageUrl ->
                ServerUtils.createEntryPoint(entryPageUrl, appContextId, settings)
            } ?: held.bbandEntryPageUrl ?: return@let null
            val compatibleServiceIds = held.coupledServices ?: emptyList()
            val application = applications?.firstOrNull { app ->
                app.appContextIdList.contains(appContextId) && app.packageName == held.bcastEntryPackageUrl
            }

            AppData(
                    appContextId,
                    ServerUtils.addSocketPath(appUrl, settings),
                    compatibleServiceIds,
                    application?.cachePath
            )
        }
    }

    override val appState = MutableLiveData(ApplicationState.UNAVAILABLE)
    override val rmpLayoutParams = MutableLiveData(RPMParams())
    override val rmpMediaUri = playbackSource.switchMap { source ->
        if (source == PlaybackSource.BROADCAST) {
            repository.routeMediaUrl.map { input ->
                if (input == null) return@map null

                if (ignoreAudioServiceMedia) {
                    val service = repository.findServiceBy(input.bsid, input.serviceId)
                    if (service?.category == SLTConstants.SERVICE_CATEGORY_AO) {
                        return@map null
                    }
                }

                fileProvider.getMediaFileUri(input.url)
            }
        } else {
            externalMediaUrl.map { input -> input?.toUri() }
        }
    }

    override val rmpState = MutableLiveData(PlaybackState.IDLE)
    override val rmpMediaTime = MutableLiveData<Long>()
    override val rmpPlaybackRate = MutableLiveData<Float>()

    fun start(lifecycleOwner: LifecycleOwner) {
        repository.selectedService.distinctUntilChanged().observe(lifecycleOwner) {
            rmpLayoutReset()
        }
        repository.heldPackage.distinctUntilChanged().observe(lifecycleOwner) {
            rmpLayoutReset()
            if (rmpState.value == PlaybackState.PAUSED) {
                rmpResume()
            }
        }
    }

    override fun setApplicationState(state: ApplicationState) {
        appState.postValue(state)
    }

    override fun rmpLayoutReset() {
        rmpLayoutParams.postValue(RPMParams())
    }

    override fun rmpPlaybackChanged(state: PlaybackState) {
        rmpState.value = state
        reportPlaybackState(state)
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

    private fun reportPlaybackState(state: PlaybackState) {
        when (state) {
            PlaybackState.PLAYING -> {
                atsc3Analytics.startDisplayMediaContent()
            }
            PlaybackState.PAUSED,
            PlaybackState.IDLE -> {
                atsc3Analytics.finishDisplayMediaContent()
            }
        }
    }
}