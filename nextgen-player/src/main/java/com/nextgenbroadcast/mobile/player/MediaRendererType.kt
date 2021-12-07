package com.nextgenbroadcast.mobile.player

import com.google.android.exoplayer2.C

enum class MediaRendererType(
    val value: Int
) {
    AUDIO(C.TRACK_TYPE_AUDIO),
    VIDEO(C.TRACK_TYPE_VIDEO),
    TEXT(C.TRACK_TYPE_TEXT);

    companion object {
        fun valueOf(value: Int) = values().firstOrNull {
            it.value == value
        }
    }
}
