package com.nextgenbroadcast.mobile.middleware.sample.view

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.nextgenbroadcast.mobile.UriPermissionProvider
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.sample.MainActivity
import com.nextgenbroadcast.mobile.middleware.sample.R
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.RMPViewModel
import com.nextgenbroadcast.mobile.view.ReceiverMediaPlayer
import kotlinx.android.synthetic.main.receiver_player_layout.view.progress_bar
import kotlinx.android.synthetic.main.receiver_player_layout.view.receiver_media_player

open class BaseReceiverPlayerView : FrameLayout {
    private val updateMediaTimeHandler = Handler(Looper.getMainLooper())

    private var rmpViewModel: RMPViewModel? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        LayoutInflater.from(context).inflate(R.layout.receiver_player_layout, this)
    }

    fun setUriPermissionProvider(uriPermissionProvider: UriPermissionProvider?) {
        receiver_media_player.setUriPermissionProvider(uriPermissionProvider)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (isInEditMode) return

        receiver_media_player.setListener(object : ReceiverMediaPlayer.EventListener {
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
                Log.d(MainActivity.TAG, error.message ?: "")
            }

            override fun onPlaybackSpeedChanged(speed: Float) {
                rmpViewModel?.setCurrentPlaybackRate(speed)
            }
        })
    }

    fun bind(viewModel: RMPViewModel) {
        rmpViewModel = viewModel
    }

    fun unbind() {
        rmpViewModel = null
    }

    open fun isPlaying(): Boolean {
        return receiver_media_player.isPlaying
    }

    fun setPlayWhenReady(playWhenReady: Boolean) {
        receiver_media_player?.playWhenReady = playWhenReady
    }

    fun startPlayback(mpdUri: Uri) {
        receiver_media_player.visibility = View.VISIBLE
        receiver_media_player.play(mpdUri)
        progress_bar.visibility = View.GONE
    }

    fun startPlayback(mpdPath: String) {
        receiver_media_player.visibility = View.VISIBLE
        receiver_media_player.play(Uri.parse(mpdPath))
        progress_bar.visibility = View.GONE
    }

    open fun startPlayback(mmtSource: Any) {
        receiver_media_player.visibility = View.INVISIBLE
        progress_bar.visibility = View.GONE
    }

    open fun stopPlayback() {
        receiver_media_player.stop()
        progress_bar.visibility = View.VISIBLE
    }

    private val updateMediaTimeRunnable = object : Runnable {
        override fun run() {
            rmpViewModel?.setCurrentMediaTime(receiver_media_player.playbackPosition)

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
        private const val MEDIA_TIME_UPDATE_DELAY = 500L
    }
}