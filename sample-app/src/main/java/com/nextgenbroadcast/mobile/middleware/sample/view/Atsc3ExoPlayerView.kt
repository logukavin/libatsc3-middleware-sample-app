package com.nextgenbroadcast.mobile.middleware.sample.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.exoplayer2.ui.PlayerView
import com.nextgenbroadcast.mobile.middleware.sample.R

class Atsc3ExoPlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : PlayerView(context, attrs), Atsc3PlayerView {

    companion object {
        fun inflate(context: Context, root: ViewGroup): Atsc3ExoPlayerView {
            return LayoutInflater.from(context).inflate(R.layout.atsc3_exo_player_view, root, false) as Atsc3ExoPlayerView
        }
    }
}