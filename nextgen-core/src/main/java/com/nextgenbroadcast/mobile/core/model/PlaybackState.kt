package com.nextgenbroadcast.mobile.core.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class PlaybackState(
        val state: Int
) : Parcelable {

    PLAYING(0),
    PAUSED(1),
    IDLE(2);

    companion object {
        fun valueOf(state: Int): PlaybackState? {
            return values().firstOrNull { it.state == state }
        }
    }
}