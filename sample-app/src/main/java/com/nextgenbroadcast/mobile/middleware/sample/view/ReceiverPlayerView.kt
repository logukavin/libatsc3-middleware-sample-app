package com.nextgenbroadcast.mobile.middleware.sample.view

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.RMPViewModel
import com.nextgenbroadcast.mobile.player.Atsc3MediaPlayer
import com.nextgenbroadcast.mobile.player.MMTConstants

class ReceiverPlayerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {
    private val atsc3Player = Atsc3MediaPlayer(context).apply {
        resetWhenLostAudioFocus = false
    }
    private val updateMediaTimeHandler = Handler(Looper.getMainLooper())

    private var rmpViewModel: RMPViewModel? = null

    private var buffering = false

    val playbackPosition
        get() = player?.currentPosition ?: 0

    val playbackSpeed: Float
        get() = player?.playbackParameters?.speed ?: 0f

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (isInEditMode) return

        atsc3Player.setListener(object : Atsc3MediaPlayer.EventListener {
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
                LOG.d(TAG, error.message ?: "")
            }

            override fun onPlaybackSpeedChanged(speed: Float) {
                rmpViewModel?.setCurrentPlaybackRate(speed)
            }
        })
    }

    fun bind(viewModel: RMPViewModel) {
        rmpViewModel = viewModel

        viewModel.setCurrentPlayerState(atsc3Player.playbackState)
        viewModel.setCurrentPlaybackRate(playbackSpeed)
    }

    fun unbind() {
        rmpViewModel = null
    }

    fun play(mediaUri: Uri) {
        val mimeType = context.contentResolver.getType(mediaUri)

        LOG.i(TAG, String.format("play: with mediaUri: %s and mimeType: %s", mediaUri, mimeType))

        // AO service content will be played out with ForegroundService embedded player which is
        // indicated on binding service with flag EXTRA_PLAY_AUDIO_ON_BOARD
        if (mimeType == MMTConstants.MIME_MMT_AUDIO) {
            stopAndClear()
            return
        }

        atsc3Player.play(mediaUri, mimeType)

        player = atsc3Player.player?.also {
            it.addListener(object : Player.EventListener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    updateBufferingState(playbackState == Player.STATE_BUFFERING)
                }
            })
        }
    }

    fun tryReplay() {
        atsc3Player.tryReplay()
        // ensure we still observing correct player
        player = atsc3Player.player
    }

    fun pause() {
        atsc3Player.pause()
    }

    fun stop() {
        atsc3Player.stop()
    }

    fun stopAndClear() {
        atsc3Player.reset()
        player = null
        atsc3Player.clearSavedState()
    }

    private fun updateBufferingState(isBuffering: Boolean) {
        if (isBuffering) {
            if (!buffering) {
                buffering = true
                postDelayed(enableBufferingProgress, 500)
            }
        } else {
            buffering = false
            removeCallbacks(enableBufferingProgress)
            setShowBuffering(SHOW_BUFFERING_NEVER)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        removeCallbacks(enableBufferingProgress)
    }

    private val enableBufferingProgress = Runnable {
        setShowBuffering(SHOW_BUFFERING_ALWAYS)
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

    fun getTrackSelector(): DefaultTrackSelector? {
        return atsc3Player.trackSelector
    }

    companion object {
        val TAG: String = ReceiverPlayerView::class.java.simpleName

        private const val MEDIA_TIME_UPDATE_DELAY = 500L
    }
}