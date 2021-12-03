package com.nextgenbroadcast.mobile.player

import com.google.android.exoplayer2.Format

data class MediaTrackDescription(
    val format: Format,
    val selected: Boolean,
    val index: Int
)
