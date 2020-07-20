package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

data class ContentAdvisoryRatingBlockChangeNotification (
    var msgType: String? = null,
    var blocked: Boolean = false,
    var contentRating: String? = null
)