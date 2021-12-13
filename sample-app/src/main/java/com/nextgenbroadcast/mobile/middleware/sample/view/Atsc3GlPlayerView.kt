package com.nextgenbroadcast.mobile.middleware.sample.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.nextgenbroadcast.mobile.middleware.sample.R
import com.nextgenbroadcast.mobile.middleware.sample.openSlhdr.OpenSlhdrGLSurfaceView

class Atsc3GlPlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : PlayerView(context, attrs), Atsc3PlayerView {

    private lateinit var glSurfaceView: OpenSlhdrGLSurfaceView

    override fun onFinishInflate() {
        super.onFinishInflate()

        val contentFrame = findViewById<FrameLayout>(R.id.exo_content_frame)

        glSurfaceView = OpenSlhdrGLSurfaceView(context, false);

        contentFrame.addView(glSurfaceView)
    }

    override fun setPlayer(player: Player?) {
        glSurfaceView.setVideoComponent(player?.videoComponent)

        super.setPlayer(player)
    }

    companion object {
        fun inflate(context: Context, root: ViewGroup): Atsc3GlPlayerView {
            return LayoutInflater.from(context).inflate(R.layout.atsc3_gl_player_view, root, false) as Atsc3GlPlayerView
        }
    }
}