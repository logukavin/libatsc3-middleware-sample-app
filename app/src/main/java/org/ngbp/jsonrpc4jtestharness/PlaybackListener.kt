package org.ngbp.jsonrpc4jtestharness

interface PlaybackListener {
    fun onStateChanged(state: PlaybackState)
}