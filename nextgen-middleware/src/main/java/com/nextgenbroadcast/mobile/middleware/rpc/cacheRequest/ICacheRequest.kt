package com.nextgenbroadcast.mobile.middleware.rpc.cacheRequest

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcParam
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import com.nextgenbroadcast.mobile.middleware.rpc.cacheRequest.model.CacheRequest
import com.nextgenbroadcast.mobile.middleware.rpc.cacheRequest.model.QueryCacheUsage

@JsonRpcType
interface ICacheRequest {
    @JsonRpcMethod("org.atsc.CacheRequest")
    fun cacheRequest(
            @JsonRpcParam("sourceURL") sourceURL: String?,
            @JsonRpcParam("targetURL") targetURL: String?,
            @JsonRpcParam("URLs") URLs: List<String>,
            @JsonRpcParam("filters") filters : List<String>?
    ): CacheRequest

    @JsonRpcMethod("org.atsc.CacheRequestDASH")
    fun CacheRequestDASH(): CacheRequest

    @JsonRpcMethod("org.atsc.cacheUsage")
    fun queryCacheUsage(): QueryCacheUsage
}