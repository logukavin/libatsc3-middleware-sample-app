package com.nextgenbroadcast.mobile.middleware.rpc.cacheRequest

import com.nextgenbroadcast.mobile.middleware.rpc.cacheRequest.model.CacheRequest
import com.nextgenbroadcast.mobile.middleware.rpc.cacheRequest.model.QueryCacheUsage

class CacheRequestImpl : ICacheRequest {
    override fun cacheRequest(): CacheRequest {
        return CacheRequest()
    }

    override fun CacheRequestDASH(): CacheRequest {
        return CacheRequest()
    }

    override fun queryCacheUsage(): QueryCacheUsage {
        return QueryCacheUsage()
    }
}