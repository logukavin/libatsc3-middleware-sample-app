package com.nextgenbroadcast.mobile.view

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.nextgenbroadcast.mobile.core.AppUtils
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.presentation.UriPermissionsObtainedListener
import java.io.IOException

class ReceiverMediaPlayer @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {

    private lateinit var simpleExoPlayer: SimpleExoPlayer
    private lateinit var dashMediaSourceFactory: DashMediaSource.Factory

    private var rmpState: PlaybackState? = null
    private var listener: EventListener? = null

    val isPlaying: Boolean
        get() = rmpState == PlaybackState.PLAYING

    val playbackPosition
        get() = simpleExoPlayer.currentPosition

    var playWhenReady
        get() = simpleExoPlayer.playWhenReady
        set(value) {
            simpleExoPlayer.playWhenReady = value
        }

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (isInEditMode) return

        simpleExoPlayer = createExoPlayer().also {
            player = it
        }

        dashMediaSourceFactory = createMediaSourceFactory()
    }

    fun setListener(listener: EventListener) {
        this.listener = listener
    }

    fun play(mediaUri: Uri) {
        val dashMediaSource = dashMediaSourceFactory.createMediaSource(mediaUri)
        player = simpleExoPlayer
        simpleExoPlayer.prepare(dashMediaSource)
        simpleExoPlayer.playWhenReady = true
    }

    fun stop() {
        simpleExoPlayer.stop()
        player = null
    }

    fun reset() {
        with(simpleExoPlayer) {
            stop()
            release()
        }
    }

    private fun createExoPlayer(): SimpleExoPlayer {
        return ExoPlayerFactory.newSimpleInstance(context).apply {
            addListener(object : Player.EventListener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    val state = when (playbackState) {
                        Player.STATE_BUFFERING, Player.STATE_READY -> {
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
                }

                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                    listener?.onPlaybackSpeedChanged(playbackParameters.speed)
                }
            })
        }
    }

    private fun createMediaSourceFactory(): DashMediaSource.Factory {
        val userAgent = AppUtils.getUserAgent(context)
        val manifestDataSourceFactory = DefaultDataSourceFactory(context, userAgent)
        val mediaDataSourceFactory = CustomDataSourceFactory(context, object : UriPermissionsListener {
            override fun onNeedPermissions(uri: Uri, callback: UriPermissionsObtainedListener) {
                listener?.onNeedPermission(uri, callback)
            }

        }, userAgent)
        return DashMediaSource.Factory(
                DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                manifestDataSourceFactory
        ).apply {
            setLoadErrorHandlingPolicy(object : DefaultLoadErrorHandlingPolicy() {
                override fun getRetryDelayMsFor(dataType: Int, loadDurationMs: Long, exception: IOException?, errorCount: Int): Long {
                    Log.w("ExoPlayerCustomLoadErrorHandlingPolicy", "dataType: $dataType, loadDurationMs: $loadDurationMs, exception ex: $exception, errorCount: $errorCount")

                    //jjustman-2019-11-07 - retry every 1s for exoplayer errors from ROUTE/DASH
                    return 1000
                }

                override fun getMinimumLoadableRetryCount(dataType: Int): Int {
                    return 1
                }
            })
        }
    }

    interface EventListener {
        fun onPlayerStateChanged(state: PlaybackState) {}
        fun onPlayerError(error: Exception) {}
        fun onPlaybackSpeedChanged(speed: Float) {}
        fun onNeedPermission(uri: Uri, callback: UriPermissionsObtainedListener)
    }
}