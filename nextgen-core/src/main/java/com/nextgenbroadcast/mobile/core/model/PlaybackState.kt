package com.nextgenbroadcast.mobile.core.model

enum class PlaybackState(
    val state: Int
) {
    IDLE(-1),
    PLAYING(0),
    PAUSED(1),
    ENDED(2),
    ENCRYPTED(3);

    companion object {
        fun valueOf(state: Int): PlaybackState? {
            return values().firstOrNull { it.state == state }
        }
    }
}