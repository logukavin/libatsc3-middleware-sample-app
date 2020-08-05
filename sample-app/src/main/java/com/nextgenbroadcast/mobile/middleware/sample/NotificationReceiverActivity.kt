package com.nextgenbroadcast.mobile.middleware.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.android.AndroidInjection
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import javax.inject.Inject

class NotificationReceiverActivity : AppCompatActivity() {
    @Inject
    lateinit var viewController: IViewController

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        parsAction()

        finish()
    }

    private fun parsAction() {
        when (intent.action) {
            PLAYER_ACTION_PLAY -> viewController.rmpResume()
            PLAYER_ACTION_PAUSE -> viewController.rmpPause()
            else -> {
            }
        }
    }

    companion object {
        const val PLAYER_ACTION_PLAY = "${BuildConfig.APPLICATION_ID}.intent.action.RMP_PLAY"
        const val PLAYER_ACTION_PAUSE = "${BuildConfig.APPLICATION_ID}.intent.action.RMP_PAUSE"
    }
}