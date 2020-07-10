package org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest.model.CacheRequest
import org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest.model.QueryCacheUsage

@JsonRpcType
interface ICacheRequest {
    @JsonRpcMethod("org.atsc.CacheRequest")
    fun cacheRequest(): CacheRequest

    @JsonRpcMethod("org.atsc.CacheRequestDASH")
    fun CacheRequestDASH(): CacheRequest

    @JsonRpcMethod("org.atsc.cacheUsage")
    fun queryCacheUsage(): QueryCacheUsage
}