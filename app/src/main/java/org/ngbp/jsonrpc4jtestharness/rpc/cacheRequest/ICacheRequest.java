package org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest;

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;

import org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest.model.QueryCacheUsage;

@JsonRpcType
public interface ICacheRequest {
    @JsonRpcMethod("org.atsc.CacheRequest")
    ICacheRequest cacheRequest();

    @JsonRpcMethod("org.atsc.CacheRequestDASH")
    ICacheRequest CacheRequestDASH();

    @JsonRpcMethod("org.atsc.cacheUsage")
    QueryCacheUsage queryCacheUsage();
}
