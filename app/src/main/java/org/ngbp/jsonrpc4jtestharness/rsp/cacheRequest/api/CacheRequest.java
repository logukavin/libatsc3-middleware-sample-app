package org.ngbp.jsonrpc4jtestharness.rsp.cacheRequest.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rsp.cacheRequest.model.QueryCacheUsage;
import org.ngbp.jsonrpc4jtestharness.rsp.mapper.JsonRpcResponse;

@JsonRpcService("")
public interface CacheRequest {
    @JsonRpcMethod("org.atsc.CacheRequest")
    JsonRpcResponse<CacheRequest> cacheRequest ();

    @JsonRpcMethod("org.atsc.CacheRequestDASH")
    JsonRpcResponse<CacheRequest> CacheRequestDASH ();

    @JsonRpcMethod("org.atsc.cacheUsage")
    JsonRpcResponse<QueryCacheUsage> queryCacheUsage ();
}
