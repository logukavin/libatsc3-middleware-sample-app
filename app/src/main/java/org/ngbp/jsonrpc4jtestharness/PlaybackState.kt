package org.ngbp.jsonrpc4jtestharness

enum class PlaybackState(val state: Int) {
    PLAYING(0),
    PAUSED(1),
    IDLE(2)
}