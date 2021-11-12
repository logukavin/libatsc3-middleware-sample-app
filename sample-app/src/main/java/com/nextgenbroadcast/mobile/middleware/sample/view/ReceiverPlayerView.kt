package com.nextgenbroadcast.mobile.middleware.sample.view

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.Display
import android.view.WindowManager
import android.widget.FrameLayout
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.nextgenbroadcast.mobile.core.LOG
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.sample.R
import com.nextgenbroadcast.mobile.middleware.sample.exoplayer.MiddlewareSlhdrRenderersFactory
import com.nextgenbroadcast.mobile.player.Atsc3MediaPlayer
import com.nextgenbroadcast.mobile.player.MMTConstants

typealias OnPlaybackChangeListener = (state: PlaybackState, position: Long, rate: Float) -> Unit

class ReceiverPlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val atsc3Player = Atsc3MediaPlayer(context).apply {
        resetWhenLostAudioFocus = false
    }

    private lateinit var exoPlayerView: Atsc3ExoPlayerView
    private lateinit var hdrPlayerView: Atsc3SlhdrPlayerView

    private var hasHdr10Display = false
    private var slhdr1Enabled = false

    private var buffering = false
    private var onPlaybackChangeListener: OnPlaybackChangeListener? = null

    private var playerView: Atsc3PlayerView? = null

    private val supportSlHdr1: Boolean
        get() = hasHdr10Display && slhdr1Enabled

    val isActive: Boolean
        get() = playerView != null

    // required for USE_DISPLAY_ADAPTATION_HACK
    private var displayTargetAdaptation: Int = 100

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (isInEditMode) return

        // SL-HDR to SDR output is not supported in the current SDK
        // check if the device has an HDR10 capable display, if not just
        // don't try to use the SL-HDR enabled GPU path
        // SL-HDR to SDR output is not supported in the current SDK
        // check if the device has an HDR10 capable display, if not just
        // don't try to use the SL-HDR enabled GPU path
        hasHdr10Display = deviceHasHdr10Display(context)

        slhdr1Enabled = resources.getBoolean(R.bool.slhdr1Enabled)

        atsc3Player.setListener(object : Atsc3MediaPlayer.EventListener {
            override fun onPlayerStateChanged(state: PlaybackState) {
                onPlaybackChangeListener?.invoke(state, atsc3Player.playbackPosition, atsc3Player.playbackSpeed)

                if (state == PlaybackState.PLAYING) {
                    keepScreenOn = true
                    startMediaTimeUpdate()
                } else {
                    keepScreenOn = false
                    cancelMediaTimeUpdate()
                }
            }

            override fun onPlayerError(error: Exception) {
                LOG.d(TAG, error.message ?: "")
            }

            override fun onPlaybackSpeedChanged(speed: Float) {
                // will be updated with position in timer
            }

            override fun onSlHdr1Present() {
                post {
                    preparePlayerView(supportSlHdr1)
                }
            }
        })

        exoPlayerView = Atsc3ExoPlayerView.inflate(context, this)
        if (supportSlHdr1) {
            hdrPlayerView = Atsc3SlhdrPlayerView.inflate(context, this)
            displayTargetAdaptation = hdrPlayerView.displayAdaptation
        }
    }

    fun setOnPlaybackChangeListener(listener: OnPlaybackChangeListener) {
        onPlaybackChangeListener = listener
    }

    fun play(mediaUri: Uri) {
        if (atsc3Player.lastMediaUri == mediaUri && (atsc3Player.isPlaying || atsc3Player.isPaused)) return

        val mimeType = context.contentResolver.getType(mediaUri)

        LOG.i(TAG, String.format("play: with mediaUri: %s and mimeType: %s", mediaUri, mimeType))

        // AO service content will be played out with ForegroundService embedded player which is
        // indicated on binding service with flag EXTRA_PLAY_AUDIO_ON_BOARD
        if (mimeType == MMTConstants.MIME_MMT_AUDIO) {
            stopAndClear()
            return
        }

        preparePlayerView(false)
        if (supportSlHdr1) {
            val renderersFactory = MiddlewareSlhdrRenderersFactory(context, hdrPlayerView.rendererConnectNtf)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            atsc3Player.play(renderersFactory, mediaUri, mimeType)
        } else {
            atsc3Player.play(mediaUri, mimeType)
        }
        atsc3Player.player?.also { player ->
            player.addListener(object : Player.EventListener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    updateBufferingState(playbackState == Player.STATE_BUFFERING)
                }
            })
        }
    }

    private fun preparePlayerView(slHdr1Compatible: Boolean) {
        // if slHdr1Compatible is false Player will throw an SlHdr1DetectedException exception is SL-HDR1 video present
        atsc3Player.slHdr1Compatible = slHdr1Compatible || !supportSlHdr1
        if (slHdr1Compatible) {
            if (USE_DISPLAY_ADAPTATION_HACK) {
                if (playerView != hdrPlayerView) {
                    removeAllViews()
                    addView(hdrPlayerView)
                    playerView = hdrPlayerView
                }
                hdrPlayerView.displayAdaptation = displayTargetAdaptation
                hdrPlayerView.gammaOutputForSdr = false
            } else {
                if (playerView != hdrPlayerView) {
                    // unused surface must be removed for the view stack using otherwise one will block the other
                    removeAllViews()
                    addView(hdrPlayerView)
                    //instead switchTargetView()
                    playerView = hdrPlayerView.apply {
                        player = atsc3Player.player
                        exoPlayerView.player = null
                    }
                }
            }
        } else {
            if (USE_DISPLAY_ADAPTATION_HACK && supportSlHdr1) {
                if (playerView != hdrPlayerView) {
                    removeAllViews()
                    addView(hdrPlayerView)
                    playerView = hdrPlayerView
                }
                hdrPlayerView.displayAdaptation = 100
                hdrPlayerView.gammaOutputForSdr = true
            } else {
                if (playerView != exoPlayerView) {
                    // unused surface must be removed for the view stack using otherwise one will block the other
                    removeAllViews()
                    addView(exoPlayerView)
                    playerView = exoPlayerView.apply {
                        player = atsc3Player.player
                        if (supportSlHdr1) {
                            hdrPlayerView.player = null
                        }
                    }
                }
            }
        }
    }

    fun tryReplay() {
        if (atsc3Player.isPlaying) return

        atsc3Player.tryReplay()
        // ensure we still observing correct player
        playerView?.setPlayer(atsc3Player.player)
    }

    fun pause() {
        atsc3Player.pause()
    }

    fun stop() {
        atsc3Player.stop()
    }

    fun stopAndClear() {
        atsc3Player.reset()
        playerView?.setPlayer(null)
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
            playerView?.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        removeCallbacks(enableBufferingProgress)
    }

    //TODO: replace with custom implementation in Atsc3Player
    fun getTrackSelector(): TrackSelector? {
        return atsc3Player.trackSelector
    }

    //TODO: replace with custom implementation in Atsc3Player
    fun getCurrentTrackSelections(): TrackSelectionArray? {
        return atsc3Player.player?.currentTrackSelections
    }

    private val enableBufferingProgress = Runnable {
        playerView?.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
    }

    private val updateMediaTimeRunnable = object : Runnable {
        override fun run() {
            onPlaybackChangeListener?.invoke(atsc3Player.playbackState, atsc3Player.playbackPosition, atsc3Player.playbackSpeed)

            postDelayed(this, MEDIA_TIME_UPDATE_DELAY)
        }
    }

    private fun startMediaTimeUpdate() {
        removeCallbacks(updateMediaTimeRunnable)
        postDelayed(updateMediaTimeRunnable, MEDIA_TIME_UPDATE_DELAY)
    }

    private fun cancelMediaTimeUpdate() {
        removeCallbacks(updateMediaTimeRunnable)
    }

    companion object {
        val TAG: String = ReceiverPlayerView::class.java.simpleName

        private const val MEDIA_TIME_UPDATE_DELAY = 500L

        private const val USE_DISPLAY_ADAPTATION_HACK = false

        fun deviceHasHdr10Display(ctx: Context): Boolean {
            val windowManager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val mainDisplay = windowManager.defaultDisplay
            val hdrCaps = mainDisplay.hdrCapabilities
            val myarray = hdrCaps.supportedHdrTypes
            // android wil indicate which HDR technologies are supported
            for (i in myarray.indices) {
                if (myarray[i] == Display.HdrCapabilities.HDR_TYPE_HDR10) {
                    return true
                }
            }
            return false
        }
    }
}