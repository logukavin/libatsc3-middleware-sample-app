package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class RatingLevel(
        var rating: String? = null,
        var contentRating: String? = null,
        var blocked: Boolean = false
) : RpcResponse()