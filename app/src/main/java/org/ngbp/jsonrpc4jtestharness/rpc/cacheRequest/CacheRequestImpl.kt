package org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest

import org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest.model.CacheRequest
import org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest.model.QueryCacheUsage

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