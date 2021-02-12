package com.nextgenbroadcast.mobile.player

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.google.android.exoplayer2.util.Util
import com.nextgenbroadcast.mmt.exoplayer2.ext.MMTLoadControl
import com.nextgenbroadcast.mmt.exoplayer2.ext.MMTMediaSource
import com.nextgenbroadcast.mmt.exoplayer2.ext.MMTRenderersFactory
import com.nextgenbroadcast.mobile.core.atsc3.mmt.MMTConstants
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.player.exoplayer.Atsc3ContentDataSource
import com.nextgenbroadcast.mobile.player.exoplayer.Atsc3MMTExtractor
import com.nextgenbroadcast.mobile.player.exoplayer.RouteDASHLoadControl
import java.io.IOException

class Atsc3MediaPlayer(
        private val context: Context
) {
    interface EventListener {
        fun onPlayerStateChanged(state: PlaybackState) {}
        fun onPlayerError(error: Exception) {}
        fun onPlaybackSpeedChanged(speed: Float) {}
    }

    private var listener: EventListener? = null

    private var _player: SimpleExoPlayer? = null
    private var isMMTPlayback = false
    private var rmpState: PlaybackState? = null

    val player: Player?
        get() = _player

    var playWhenReady: Boolean = true
        set(value) {
            _player?.playWhenReady = value
            field = value
        }

    val isPlaying: Boolean
        get() = _player?.isPlaying ?: false

    val playbackState: PlaybackState
        get() = _player?.let {
            playbackState(it.playbackState, it.playWhenReady)
        } ?: PlaybackState.IDLE

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

            _player = createMMTExoPlayer().apply {
                prepare(mediaSource)
                playWhenReady = this@Atsc3MediaPlayer.playWhenReady
            }
        } else {
            val dashMediaSource = createMediaSourceFactory().createMediaSource(mediaUri)
            _player = createDefaultExoPlayer().apply {
                prepare(dashMediaSource)
                playWhenReady = true
            }
        }
    }

    fun reset() {
        _player?.let {
            it.stop()
            it.release()
            _player = null
        }
        isMMTPlayback = false
    }

    private fun createMediaSourceFactory(): DashMediaSource.Factory {
        val userAgent = Util.getUserAgent(context, AppUtils.getUserAgent(context))
        val manifestDataSourceFactory = DefaultDataSourceFactory(context, userAgent)
        val mediaDataSourceFactory = DefaultDataSourceFactory(context, userAgent)

        return DashMediaSource.Factory(
                DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                manifestDataSourceFactory
        ).apply {
            setLoadErrorHandlingPolicy(createDefaultLoadErrorHandlingPolicy())
        }
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
//                    updateBufferingState(playbackState == Player.STATE_BUFFERING)

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
}