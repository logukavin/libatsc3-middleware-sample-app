package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

data class CaptionStateChangeNotification (
    var msgType: String? = null,
    var captionDisplay: Boolean = false
)