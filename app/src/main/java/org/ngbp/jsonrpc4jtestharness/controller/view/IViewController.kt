package org.ngbp.jsonrpc4jtestharness.controller.view

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.presentation.IMediaPlayerPresenter
import org.ngbp.jsonrpc4jtestharness.presentation.IUserAgentPresenter

interface IViewController : IUserAgentPresenter, IMediaPlayerPresenter {
    val rmpState: LiveData<PlaybackState>

    fun updateRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double)

    fun rmpPause()
    fun rmpResume()

    fun requestMediaPlay(mediaUrl: String?, delay: Long)
    fun requestMediaStop(delay: Long)
}