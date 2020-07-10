package org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class QueryCacheUsage(
        var usageSize: Int? = null,
        var quotaSize: Int? = null
) : RpcResponse()
