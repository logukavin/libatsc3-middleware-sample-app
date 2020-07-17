package org.ngbp.jsonrpc4jtestharness

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.android.AndroidInjection
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.lifecycle.RMPViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.factory.UserAgentViewModelFactory
import org.ngbp.jsonrpc4jtestharness.service.NotificationHelper.Companion.PLAYER_ACTION_PAUSE
import org.ngbp.jsonrpc4jtestharness.service.NotificationHelper.Companion.PLAYER_ACTION_PLAY
import javax.inject.Inject

class NotificationReceiverActivity : AppCompatActivity() {
    @Inject
    lateinit var userAgentViewModelFactory: UserAgentViewModelFactory

    private val rmpViewModel: RMPViewModel by viewModels { userAgentViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        rmpViewModel.setCurrentPlayerState(parsValue())
        finish()
    }

    private fun parsValue(): PlaybackState {
        return when (intent.action) {
            PLAYER_ACTION_PLAY -> PlaybackState.PLAYING
            PLAYER_ACTION_PAUSE -> PlaybackState.PAUSED
            else -> PlaybackState.IDLE
        }
    }
}