package org.ngbp.jsonrpc4jtestharness.controller

import org.ngbp.jsonrpc4jtestharness.PlaybackState

interface IRPCController {
    var language: String
    var queryServiceId: String?
    var mediaUrl: String?
    var playbackState: PlaybackState

    fun updateViewPosition(scaleFactor: Double?, xPos: Double?, yPos: Double?)
}