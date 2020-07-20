package org.ngbp.jsonrpc4jtestharness

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.android.AndroidInjection
import org.ngbp.jsonrpc4jtestharness.lifecycle.RMPViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.factory.UserAgentViewModelFactory
import javax.inject.Inject

class NotificationReceiverActivity : AppCompatActivity() {
    @Inject
    lateinit var userAgentViewModelFactory: UserAgentViewModelFactory

    private val rmpViewModel: RMPViewModel by viewModels { userAgentViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        parsAction()

        finish()
    }

    private fun parsAction() {
        when (intent.action) {
            PLAYER_ACTION_PLAY -> rmpViewModel.restorePlayback()
            PLAYER_ACTION_PAUSE -> rmpViewModel.pausePlayback()
            else -> {
            }
        }
    }

    companion object {
        const val PLAYER_ACTION_PLAY = "${BuildConfig.APPLICATION_ID}.intent.action.RMP_PLAY"
        const val PLAYER_ACTION_PAUSE = "${BuildConfig.APPLICATION_ID}.intent.action.RMP_PAUSE"
    }
}