package org.ngbp.jsonrpc4jtestharness.controller

import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState

interface IRPCController {
    val language: String
    val queryServiceId: String?
    val mediaUrl: String?
    val playbackState: PlaybackState
    var rmpUrl: String?
    var rmpOperation: String?
    var currentMPD: String?
    var rmpSyncTime: Double?

    fun updateRMPPosition(scaleFactor: Double?, xPos: Double?, yPos: Double?)
    fun updateRMPState(state: PlaybackState)
}