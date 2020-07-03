package org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest.model.QueryCacheUsage

@JsonRpcType
interface ICacheRequest {
    @JsonRpcMethod("org.atsc.CacheRequest")
    fun cacheRequest(): ICacheRequest?

    @JsonRpcMethod("org.atsc.CacheRequestDASH")
    fun CacheRequestDASH(): ICacheRequest?

    @JsonRpcMethod("org.atsc.cacheUsage")
    fun queryCacheUsage(): QueryCacheUsage?
}