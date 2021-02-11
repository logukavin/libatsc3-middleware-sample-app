package com.nextgenbroadcast.mobile.middleware.sample.view

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.RMPViewModel
import com.nextgenbroadcast.mobile.view.ReceiverMediaPlayerView

class ReceiverPlayerLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ReceiverMediaPlayerView(context, attrs, defStyleAttr) {
    private val updateMediaTimeHandler = Handler(Looper.getMainLooper())

    private var rmpViewModel: RMPViewModel? = null

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (isInEditMode) return

        setListener(object : EventListener {
            override fun onPlayerStateChanged(state: PlaybackState) {
                rmpViewModel?.setCurrentPlayerState(state)

                if (state == PlaybackState.PLAYING) {
                    //window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    keepScreenOn = true
                    startMediaTimeUpdate()
                } else {
                    //window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    keepScreenOn = false
                    cancelMediaTimeUpdate()
                }
            }

            override fun onPlayerError(error: Exception) {
                Log.d(TAG, error.message ?: "")
            }

            override fun onPlaybackSpeedChanged(speed: Float) {
                rmpViewModel?.setCurrentPlaybackRate(speed)
            }
        })
    }

    fun bind(viewModel: RMPViewModel) {
        rmpViewModel = viewModel

        viewModel.setCurrentPlayerState(playbackState)
        viewModel.setCurrentPlaybackRate(playbackSpeed)
    }

    fun unbind() {
        rmpViewModel = null
    }

    private val updateMediaTimeRunnable = object : Runnable {
        override fun run() {
            rmpViewModel?.setCurrentMediaTime(playbackPosition)

            updateMediaTimeHandler.postDelayed(this, MEDIA_TIME_UPDATE_DELAY)
        }
    }

    private fun startMediaTimeUpdate() {
        updateMediaTimeHandler.removeCallbacks(updateMediaTimeRunnable)
        updateMediaTimeHandler.postDelayed(updateMediaTimeRunnable, MEDIA_TIME_UPDATE_DELAY)
    }

    private fun cancelMediaTimeUpdate() {
        updateMediaTimeHandler.removeCallbacks(updateMediaTimeRunnable)
    }

    companion object {
        val TAG: String = ReceiverPlayerLayout::class.java.simpleName

        private const val MEDIA_TIME_UPDATE_DELAY = 500L
    }
}