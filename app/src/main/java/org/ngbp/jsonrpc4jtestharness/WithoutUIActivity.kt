package org.ngbp.jsonrpc4jtestharness

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.android.AndroidInjection
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.lifecycle.RMPViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.factory.UserAgentViewModelFactory
import org.ngbp.jsonrpc4jtestharness.service.NotificationHelper
import javax.inject.Inject

class WithoutUIActivity : AppCompatActivity() {
    @Inject
    lateinit var userAgentViewModelFactory: UserAgentViewModelFactory

    private val rmpViewModel: RMPViewModel by viewModels { userAgentViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        rmpViewModel.setState(parsValue())
        finish()
    }

    private fun parsValue(): PlaybackState {
        return when (intent.extras?.getInt(NotificationHelper.PLAYER_ACTION)) {
            0 -> PlaybackState.PLAYING
            1 -> PlaybackState.PAUSED
            else -> PlaybackState.IDLE
        }
    }
}