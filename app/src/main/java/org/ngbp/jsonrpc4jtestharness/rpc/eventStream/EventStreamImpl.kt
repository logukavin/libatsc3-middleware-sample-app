package org.ngbp.jsonrpc4jtestharness.rpc.eventStream

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

class EventStreamImpl : IEventStream {
    override fun eventStreamSubscribe(): RpcResponse {
        return RpcResponse()
    }

    override fun eventStreamUnsubscribe(): RpcResponse {
        return RpcResponse()
    }

    override fun eventStreamEvent(): RpcResponse {
        return RpcResponse()
    }
}