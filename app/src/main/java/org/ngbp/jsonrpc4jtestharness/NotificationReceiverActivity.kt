package org.ngbp.jsonrpc4jtestharness

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.android.AndroidInjection
import org.ngbp.jsonrpc4jtestharness.controller.IMediaPlayerController
import javax.inject.Inject

class NotificationReceiverActivity : AppCompatActivity() {
    @Inject
    lateinit var mediaController: IMediaPlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        parsAction()

        finish()
    }

    private fun parsAction() {
        when (intent.action) {
            PLAYER_ACTION_PLAY -> mediaController.rmpResume()
            PLAYER_ACTION_PAUSE -> mediaController.rmpPause()
            else -> {
            }
        }
    }

    companion object {
        const val PLAYER_ACTION_PLAY = "${BuildConfig.APPLICATION_ID}.intent.action.RMP_PLAY"
        const val PLAYER_ACTION_PAUSE = "${BuildConfig.APPLICATION_ID}.intent.action.RMP_PAUSE"
    }
}