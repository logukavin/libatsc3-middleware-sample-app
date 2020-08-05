package com.nextgenbroadcast.mobile.middleware.rpc.eventStream

import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse

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