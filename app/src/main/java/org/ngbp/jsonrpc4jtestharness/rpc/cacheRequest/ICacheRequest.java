package org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest.model.QueryCacheUsage;

@JsonRpcService("")
public interface ICacheRequest {
    @JsonRpcMethod("org.atsc.CacheRequest")
    ICacheRequest cacheRequest();

    @JsonRpcMethod("org.atsc.CacheRequestDASH")
    ICacheRequest CacheRequestDASH();

    @JsonRpcMethod("org.atsc.cacheUsage")
    QueryCacheUsage queryCacheUsage();
}
