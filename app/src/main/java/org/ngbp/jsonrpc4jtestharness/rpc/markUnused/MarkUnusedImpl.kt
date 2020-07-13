package org.ngbp.jsonrpc4jtestharness.rpc.markUnused

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

class MarkUnusedImpl : IMarkUnused {
    override fun markUnused(): RpcResponse {
        return RpcResponse()
    }
}