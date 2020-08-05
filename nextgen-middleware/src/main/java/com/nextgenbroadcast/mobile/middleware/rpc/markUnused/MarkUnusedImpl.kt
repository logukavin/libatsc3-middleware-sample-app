package com.nextgenbroadcast.mobile.middleware.rpc.markUnused

import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse

class MarkUnusedImpl : IMarkUnused {
    override fun markUnused(): RpcResponse {
        return RpcResponse()
    }
}