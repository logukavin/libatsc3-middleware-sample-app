package org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest.model

data class CacheRequestDASHParams (
    var sourceURL: String? = null,
    var targetURL: String? = null,
    var Period: String? = null,
    var mpdFileName: String? = null,
    var filters: MutableList<String?>? = null
)