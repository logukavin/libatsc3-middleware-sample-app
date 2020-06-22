package org.ngbp.jsonrpc4jtestharness.rsp.cacheRequest.api;

import org.ngbp.jsonrpc4jtestharness.rsp.cacheRequest.model.QueryCacheUsage;
import org.ngbp.jsonrpc4jtestharness.rsp.mapper.JsonRpcResponse;

public class CacheRequestImpl implements CacheRequest {
    @Override
    public JsonRpcResponse<CacheRequest> cacheRequest() {
        return null;
    }

    @Override
    public JsonRpcResponse<CacheRequest> CacheRequestDASH() {
        return null;
    }

    @Override
    public JsonRpcResponse<QueryCacheUsage> queryCacheUsage() {
        return null;
    }
}
