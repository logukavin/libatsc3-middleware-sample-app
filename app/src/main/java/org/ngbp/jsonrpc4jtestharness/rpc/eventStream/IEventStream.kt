package org.ngbp.jsonrpc4jtestharness.rpc.eventStream

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

@JsonRpcType
interface IEventStream {
    @JsonRpcMethod("org.atsc.eventStream.subscribe")
    fun eventStreamSubscribe(): RpcResponse

    @JsonRpcMethod("org.atsc.eventStream.unsubscribe")
    fun eventStreamUnsubscribe(): RpcResponse

    @JsonRpcMethod("org.atsc.eventStream.unsubscribe")
    fun eventStreamEvent(): RpcResponse
}