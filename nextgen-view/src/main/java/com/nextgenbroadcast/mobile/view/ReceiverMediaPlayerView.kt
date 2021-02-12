package com.nextgenbroadcast.mobile.view

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.nextgenbroadcast.mmt.exoplayer2.ext.MMTLoadControl
import com.nextgenbroadcast.mmt.exoplayer2.ext.MMTMediaSource
import com.nextgenbroadcast.mmt.exoplayer2.ext.MMTRenderersFactory
import com.nextgenbroadcast.mobile.core.AppUtils
import com.nextgenbroadcast.mobile.core.atsc3.mmt.MMTConstants
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.mmt.exoplayer2.Atsc3MMTExtractor
import com.nextgenbroadcast.mobile.exoplayer2.RouteDASHLoadControl
import com.nextgenbroadcast.mobile.mmt.exoplayer2.Atsc3ContentDataSource
import java.io.IOException

//TODO: reuse Atsc3MediaPlayer
open class ReceiverMediaPlayerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {

    private var buffering = false
    private val enableBufferingProgress = Runnable {
        setShowBuffering(SHOW_BUFFERING_ALWAYS)
    }

    private val dashMediaSourceFactory: DashMediaSource.Factory by lazy {
        createMediaSourceFactory()
    }

    private var rmpState: PlaybackState? = null
    private var listener: EventListener? = null
    private var isMMTPlayback = false

    val isPlaying: Boolean
        get() = rmpState == PlaybackState.PLAYING

    val playbackPosition
        get() = player?.currentPosition ?: 0

    val playbackState: PlaybackState
        get() = player?.let {
            playbackState(it.playbackState, it.playWhenReady)
        } ?: PlaybackState.IDLE

    val playbackSpeed: Float
        get() = player?.playbackParameters?.speed ?: 0f

    var playWhenReady
        get() = player?.playWhenReady ?: false
        set(value) {
            player?.playWhenReady = value
        }

    fun setListener(listener: EventListener) {
        this.listener = listener
    }

    fun play(mediaUri: Uri) {
        reset()

        val mimeType = context.contentResolver.getType(mediaUri)
        if (mimeType == MMTConstants.MIME_MMT_VIDEO || mimeType == MMTConstants.MIME_MMT_AUDIO) {
            isMMTPlayback = true

            val mediaSource = MMTMediaSource.Factory({
                Atsc3ContentDataSource(context)
            }, {
                arrayOf(Atsc3MMTExtractor())
            }).apply {
                setLoadErrorHandlingPolicy(createDefaultLoadErrorHandlingPolicy())
            }.createMediaSource(mediaUri)

            player = createMMTExoPlayer().apply {
                prepare(mediaSource)
                playWhenReady = true
            }
        } else {
            val dashMediaSource = dashMediaSourceFactory.createMediaSource(mediaUri)
            player = createDefaultExoPlayer().apply {
                prepare(dashMediaSource)
                playWhenReady = true
            }
        }
    }

    fun stop() {
        reset()
    }

    private fun reset() {
        player?.let {
            it.stop()
            it.release()
            player = null
        }
        isMMTPlayback = false
    }

    private fun createDefaultExoPlayer(): SimpleExoPlayer {
        return createExoPlayer(RouteDASHLoadControl(), DefaultRenderersFactory(context))
    }

    private fun createMMTExoPlayer(): SimpleExoPlayer {
        return createExoPlayer(MMTLoadControl(), MMTRenderersFactory(context))
    }

    private fun createExoPlayer(loadControl: LoadControl, renderersFactory: RenderersFactory): SimpleExoPlayer {
        return ExoPlayerFactory.newSimpleInstance(context, renderersFactory, DefaultTrackSelector(), loadControl).apply {
            addListener(object : Player.EventListener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    updateBufferingState(playbackState == Player.STATE_BUFFERING)

                    val state = playbackState(playbackState, playWhenReady) ?: return
                    if (rmpState != state) {
                        rmpState = state
                        listener?.onPlayerStateChanged(state)
                    }
                }

                override fun onPlayerError(error: ExoPlaybackException) {
                    listener?.onPlayerError(error)

                    if (isMMTPlayback) {
                        seekToDefaultPosition()
                    }

                    retry()
                }

                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                    listener?.onPlaybackSpeedChanged(playbackParameters.speed)
                }
            })
        }
    }

    private fun playbackState(playbackState: Int, playWhenReady: Boolean): PlaybackState? {
        return when (playbackState) {
            Player.STATE_BUFFERING, Player.STATE_READY -> {
                if (playWhenReady) PlaybackState.PLAYING else PlaybackState.PAUSED
            }
            Player.STATE_IDLE, Player.STATE_ENDED -> {
                PlaybackState.IDLE
            }
            else -> null
        }
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

    private fun createMediaSourceFactory(): DashMediaSource.Factory {
        val userAgent = AppUtils.getUserAgent(context)
        val manifestDataSourceFactory = DefaultDataSourceFactory(context, userAgent)
        val mediaDataSourceFactory = DefaultDataSourceFactory(context, userAgent)

        return DashMediaSource.Factory(
                DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                manifestDataSourceFactory
        ).apply {
            setLoadErrorHandlingPolicy(createDefaultLoadErrorHandlingPolicy())
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        removeCallbacks(enableBufferingProgress)
    }

    private fun createDefaultLoadErrorHandlingPolicy(): DefaultLoadErrorHandlingPolicy {
        return object : DefaultLoadErrorHandlingPolicy() {
            override fun getRetryDelayMsFor(dataType: Int, loadDurationMs: Long, exception: IOException?, errorCount: Int): Long {
                Log.w("ExoPlayerCustomLoadErrorHandlingPolicy", "dataType: $dataType, loadDurationMs: $loadDurationMs, exception ex: $exception, errorCount: $errorCount")

                //jjustman-2019-11-07 - retry every 1s for exoplayer errors from ROUTE/DASH
                return 1000
            }

            override fun getMinimumLoadableRetryCount(dataType: Int): Int {
                return 1
            }
        }
    }

    interface EventListener {
        fun onPlayerStateChanged(state: PlaybackState) {}
        fun onPlayerError(error: Exception) {}
        fun onPlaybackSpeedChanged(speed: Float) {}
    }
}