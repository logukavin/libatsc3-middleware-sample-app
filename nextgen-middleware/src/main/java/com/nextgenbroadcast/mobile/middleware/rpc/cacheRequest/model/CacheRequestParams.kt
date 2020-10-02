package com.nextgenbroadcast.mobile.middleware.rpc.cacheRequest.model

data class CacheRequestParams (
    var sourceURL: String? = null,
    var targetURL: String? = null,
    var URLs: MutableList<String?>? = null,
    var filters: MutableList<Int?>? = null
)