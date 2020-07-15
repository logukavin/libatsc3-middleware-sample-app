package org.ngbp.jsonrpc4jtestharness

enum class PlaybackState(val state: Int) {
    Playing(0),
    Paused(1),
    Ended(2)
}