package org.ngbp.jsonrpc4jtestharness.rpc

enum class ERROR_CODES(val code: Int, val message: String) {
    PARSING_ERROR_CODE(-1, ""),
    MPD_CANNOT_BE_ACCESSED(-11, "The indicated MPD cannot be accessed"),
    CONTENT_CANNOT_BE_PLAYED(-12, "The content cannot be played"),
    MPD_ANCHOR_CANNOT_BE_REACHED(-13, "The requested MPD Anchor cannot be reached"),
    SYNCHRONIZATION_CANNOT_BE_ACHIEVED(-19, "The synchronization specified by rmpSyncTime cannot be achieved")
}