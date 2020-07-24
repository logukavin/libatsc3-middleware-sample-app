package org.ngbp.jsonrpc4jtestharness.core.model

enum class PlaybackState(val state: Int) {
    PLAYING(0),
    PAUSED(1),
    IDLE(2);

    companion object {
        fun valueOf(state: Int): PlaybackState? {
            return values().first { it.state == state }
        }
    }
}