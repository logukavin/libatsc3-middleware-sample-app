package com.nextgenbroadcast.mobile.view

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import androidx.core.net.toUri
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.nextgenbroadcast.mobile.core.AppUtils
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.permission.AlterDataSourceFactory
import com.nextgenbroadcast.mobile.permission.UriPermissionProvider
import com.nextgenbroadcast.mobile.mmt.atsc3.media.MMTDataBuffer
import com.nextgenbroadcast.mobile.mmt.exoplayer2.MMTDataSource
import com.nextgenbroadcast.mobile.mmt.exoplayer2.MMTExtractor
import com.nextgenbroadcast.mobile.mmt.exoplayer2.MMTLoadControl
import com.nextgenbroadcast.mobile.exoplayer2.RouteDASHLoadControl
import java.io.IOException

class ReceiverMediaPlayer @JvmOverloads constructor(
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
    private var uriPermissionProvider: UriPermissionProvider? = null
    private var isMMTPlayback = false

    val isPlaying: Boolean
        get() = rmpState == PlaybackState.PLAYING

    val playbackPosition
        get() = player?.currentPosition ?: 0

    var playWhenReady
        get() = player?.playWhenReady ?: false
        set(value) {
            player?.playWhenReady = value
        }

    fun setListener(listener: EventListener) {
        this.listener = listener
    }

    fun setUriPermissionProvider(uriPermissionProvider: UriPermissionProvider?) {
        this.uriPermissionProvider = uriPermissionProvider
    }

    fun play(mediaUri: Uri) {
        reset()

        val dashMediaSource = dashMediaSourceFactory.createMediaSource(mediaUri)
        player = createDefaultExoPlayer().apply {
            prepare(dashMediaSource)
            playWhenReady = true
        }
    }

    fun play(mmtBuffer: MMTDataBuffer) {
        reset()

        isMMTPlayback = true

        val mediaSource = ProgressiveMediaSource.Factory({
            MMTDataSource(mmtBuffer)
        }, {
            arrayOf(MMTExtractor())
        }).apply {
            setLoadErrorHandlingPolicy(createDefaultLoadErrorHandlingPolicy())
        }.createMediaSource("mmt".toUri())

        player = createMMTExoPlayer().apply {
            prepare(mediaSource)
            playWhenReady = true
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
        return createExoPlayer(RouteDASHLoadControl())
    }

    private fun createMMTExoPlayer(): SimpleExoPlayer {
        return createExoPlayer(MMTLoadControl())
    }

    private fun createExoPlayer(loadControl: LoadControl): SimpleExoPlayer {
        return ExoPlayerFactory.newSimpleInstance(context, DefaultRenderersFactory(context), DefaultTrackSelector(), loadControl).apply {
            addListener(object : Player.EventListener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    val state = when (playbackState) {
                        Player.STATE_BUFFERING, Player.STATE_READY -> {
                            updateBufferingState(playbackState == Player.STATE_BUFFERING)
                            if (playWhenReady) PlaybackState.PLAYING else PlaybackState.PAUSED
                        }
                        Player.STATE_IDLE, Player.STATE_ENDED -> {
                            PlaybackState.IDLE
                        }
                        else -> return
                    }

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
        val mediaDataSourceFactory = AlterDataSourceFactory(context, userAgent, uriPermissionProvider)

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