package com.nextgenbroadcast.mobile.middleware.controller.view

import android.net.Uri
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.RPMParams
import com.nextgenbroadcast.mobile.core.presentation.ApplicationState
import com.nextgenbroadcast.mobile.core.presentation.media.IObservablePlayer
import kotlinx.coroutines.flow.StateFlow

internal interface IViewController {
    val sessionNum: StateFlow<Int>

    val appData: StateFlow<AppData?>
    val appState: StateFlow<ApplicationState>

    val rmpState: StateFlow<PlaybackState>
    val rmpMediaUri: StateFlow<Uri?>
    val rmpLayoutParams: StateFlow<RPMParams>
    val rmpMediaTime: StateFlow<Long>
    val rmpPlaybackRate: StateFlow<Float>

    fun onNewSessionStarted()

    fun setApplicationState(state: ApplicationState)

    fun updateRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double)

    fun rmpStop()
    fun rmpPause()
    fun rmpResume()
    fun rmpLayoutReset()
    fun rmpPlaybackChanged(state: PlaybackState)
    fun rmpPlaybackRateChanged(speed: Float)
    fun rmpMediaTimeChanged(currentTime: Long)

    //TODO: remove after migration to MediaSession
    fun addOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener)
    fun removeOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener)

    fun requestMediaPlay(mediaUrl: String?, delay: Long)
    fun requestMediaStop(delay: Long)
}