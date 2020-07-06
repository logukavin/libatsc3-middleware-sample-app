package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model

data class RatingLevel (
    var rating: String? = null,
    var contentRating: String? = null,
    var blocked: Boolean = false
)