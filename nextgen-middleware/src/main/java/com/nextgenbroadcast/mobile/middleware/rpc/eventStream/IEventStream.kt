package com.nextgenbroadcast.mobile.middleware.rpc.eventStream

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse

@JsonRpcType
interface IEventStream {
    @JsonRpcMethod("org.atsc.eventStream.subscribe")
    fun eventStreamSubscribe(): RpcResponse

    @JsonRpcMethod("org.atsc.eventStream.unsubscribe")
    fun eventStreamUnsubscribe(): RpcResponse

    @JsonRpcMethod("org.atsc.eventStream.unsubscribe")
    fun eventStreamEvent(): RpcResponse
}