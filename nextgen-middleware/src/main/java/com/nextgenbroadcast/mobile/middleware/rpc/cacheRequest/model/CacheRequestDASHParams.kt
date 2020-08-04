package com.nextgenbroadcast.mobile.middleware.rpc.cacheRequest.model

data class CacheRequestDASHParams (
    var sourceURL: String? = null,
    var targetURL: String? = null,
    var Period: String? = null,
    var mpdFileName: String? = null,
    var filters: MutableList<String?>? = null
)