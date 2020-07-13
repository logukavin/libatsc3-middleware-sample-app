package org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class CacheRequest(
        var cached: Boolean = false
) : RpcResponse()