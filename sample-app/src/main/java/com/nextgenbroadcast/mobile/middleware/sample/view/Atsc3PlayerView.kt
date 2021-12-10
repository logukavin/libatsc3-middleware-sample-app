package com.nextgenbroadcast.mobile.middleware.sample.view

import com.google.android.exoplayer2.Player

interface Atsc3PlayerView {
    fun setPlayer(player: Player?)
    fun setShowBuffering(showBuffering: Int)
}