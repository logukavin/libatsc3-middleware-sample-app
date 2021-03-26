package com.nextgenbroadcast.mobile.middleware.controller.view

import android.net.Uri
import androidx.core.net.toUri
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.RPMParams
import com.nextgenbroadcast.mobile.core.presentation.ApplicationState
import com.nextgenbroadcast.mobile.core.presentation.media.IObservablePlayer
import com.nextgenbroadcast.mobile.core.presentation.media.PlayerStateRegistry
import com.nextgenbroadcast.mobile.middleware.analytics.IAtsc3Analytics
import com.nextgenbroadcast.mobile.middleware.service.provider.IMediaFileProvider
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.server.ServerUtils
import com.nextgenbroadcast.mobile.middleware.settings.IClientSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class ViewControllerImpl(
        private val repository: IRepository,
        private val settings: IClientSettings,
        private val fileProvider: IMediaFileProvider,
        private val atsc3Analytics: IAtsc3Analytics,
        private val stateScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
        private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : IViewController {

    private enum class PlaybackSource {
        BROADCAST, BROADBAND
    }

    private val rmpListeners: PlayerStateRegistry = PlayerStateRegistry()
    private val playbackSource = MutableStateFlow(PlaybackSource.BROADCAST)
    private val externalMediaUrl = MutableStateFlow<String?>(null)

    override val sessionNum = MutableStateFlow(0)

    override val appData: StateFlow<AppData?> = combine(repository.heldPackage, repository.applications, sessionNum) { held, applications, sNum ->
        held?.let {
            val appContextId = held.appContextId ?: return@let null
            val appUrl = held.bcastEntryPageUrl?.let { entryPageUrl ->
                ServerUtils.createEntryPoint(entryPageUrl, appContextId, settings)
            } ?: held.bbandEntryPageUrl ?: return@let null
            val compatibleServiceIds = held.coupledServices ?: emptyList()
            val application = applications.firstOrNull { app ->
                app.appContextIdList.contains(appContextId) && app.packageName == held.bcastEntryPackageUrl
            }

            AppData(
                    appContextId,
                    ServerUtils.addSocketPath(appUrl, settings),
                    compatibleServiceIds,
                    application?.cachePath
            )

        }
    }.stateIn(stateScope, SharingStarted.Eagerly, null)

    override val appState = MutableStateFlow(ApplicationState.UNAVAILABLE)

    override val rmpLayoutParams = MutableStateFlow(RPMParams())
    override val rmpMediaUri: StateFlow<Uri?> = combine(playbackSource, repository.routeMediaUrl, externalMediaUrl) { source, routeMediaUrl, externalMediaUrl ->
        if (source == PlaybackSource.BROADCAST) {
            routeMediaUrl?.let {
                fileProvider.getMediaFileUri(routeMediaUrl.url)
            }
        } else {
            externalMediaUrl?.toUri()
        }
    }.stateIn(stateScope, SharingStarted.Eagerly, null)

    override val rmpState = MutableStateFlow(PlaybackState.IDLE)
    override val rmpMediaTime = MutableStateFlow(0L)
    override val rmpPlaybackRate = MutableStateFlow(0f)

    init {
        stateScope.launch {
            repository.selectedService.collect {
                rmpLayoutReset()
            }
        }

        stateScope.launch {
            repository.heldPackage.collect {
                rmpLayoutReset()
                if (rmpState.value == PlaybackState.PAUSED) {
                    rmpResume()
                }
            }
        }
    }

    override fun setApplicationState(state: ApplicationState) {
        appState.value = state
    }

    override fun rmpLayoutReset() {
        rmpLayoutParams.value = RPMParams()
    }

    override fun rmpPlaybackChanged(state: PlaybackState) {
        rmpState.value = state
        reportPlaybackState(state)
    }

    override fun rmpPause() {
        mainScope.launch {
            rmpListeners.notifyPause(null)
        }
    }

    override fun rmpResume() {
        mainScope.launch {
            rmpListeners.notifyResume(null)
        }
    }

    override fun addOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener) {
        rmpListeners.add(callback)
    }

    override fun removeOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener) {
        rmpListeners.remove(callback)
    }

    override fun updateRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double) {
        rmpLayoutParams.value = RPMParams(scaleFactor, xPos.toInt(), yPos.toInt())
    }

    override fun rmpPlaybackRateChanged(speed: Float) {
        rmpPlaybackRate.value = speed
    }

    override fun rmpMediaTimeChanged(currentTime: Long) {
        rmpMediaTime.value = currentTime
    }

    //TODO: currently delay not supported and blocked on RPC level
    override fun requestMediaPlay(mediaUrl: String?, delay: Long) {
        mainScope.launch {
            rmpPause()
            if (mediaUrl != null) {
                playbackSource.value = PlaybackSource.BROADBAND
            } else {
                playbackSource.value = PlaybackSource.BROADCAST
            }
            externalMediaUrl.value = mediaUrl
            rmpResume()
        }
    }

    //TODO: currently delay not supported and blocked on RPC level
    override fun requestMediaStop(delay: Long) {
        mainScope.launch {
            rmpPause()
        }
    }

    override fun onNewSessionStarted() {
        sessionNum.value++
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