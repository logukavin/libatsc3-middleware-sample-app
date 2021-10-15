package com.nextgenbroadcast.mobile.middleware.sample.view

import android.net.Uri
import com.google.android.exoplayer2.Player
import com.nextgenbroadcast.mobile.player.Atsc3MediaPlayer

interface Atsc3PlayerView {
    fun setPlayer(player: Player?)
    fun setShowBuffering(showBuffering: Int)

    fun play(atsc3Player: Atsc3MediaPlayer, mediaUri: Uri, mimeType: String? = null)
}