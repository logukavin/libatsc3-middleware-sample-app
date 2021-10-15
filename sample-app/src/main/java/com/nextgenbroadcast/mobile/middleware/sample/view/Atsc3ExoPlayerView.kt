package com.nextgenbroadcast.mobile.middleware.sample.view

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.exoplayer2.ui.PlayerView
import com.nextgenbroadcast.mobile.middleware.sample.R
import com.nextgenbroadcast.mobile.player.Atsc3MediaPlayer

class Atsc3ExoPlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : PlayerView(context, attrs), Atsc3PlayerView {
    override fun play(atsc3Player: Atsc3MediaPlayer, mediaUri: Uri, mimeType: String?) {
        atsc3Player.play(mediaUri, mimeType)
    }

    companion object {
        fun inflate(context: Context, root: ViewGroup): Atsc3ExoPlayerView {
            return LayoutInflater.from(context).inflate(R.layout.atsc3_exo_player_view, root, false) as Atsc3ExoPlayerView
        }
    }
}