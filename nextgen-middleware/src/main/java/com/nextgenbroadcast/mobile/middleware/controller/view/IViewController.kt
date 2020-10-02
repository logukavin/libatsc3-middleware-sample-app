package com.nextgenbroadcast.mobile.middleware.controller.view

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.core.presentation.IUserAgentPresenter

interface IViewController : IUserAgentPresenter, IMediaPlayerPresenter {
    val rmpState: LiveData<PlaybackState>
    val rmpMediaTime: LiveData<Long>
    val rmpPlaybackRate: LiveData<Float>

    fun updateRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double)

    fun rmpPause()
    fun rmpResume()

    fun requestMediaPlay(mediaUrl: String?, delay: Long)
    fun requestMediaStop(delay: Long)
}