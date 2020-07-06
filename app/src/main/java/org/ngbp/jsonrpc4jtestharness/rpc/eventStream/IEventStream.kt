package org.ngbp.jsonrpc4jtestharness.rpc.eventStream

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType

@JsonRpcType
interface IEventStream {
    @JsonRpcMethod("org.atsc.eventStream.subscribe")
    fun eventStreamSubscribe(): Any?

    @JsonRpcMethod("org.atsc.eventStream.unsubscribe")
    fun eventStreamUnsubscribe(): Any?

    @JsonRpcMethod("org.atsc.eventStream.unsubscribe")
    fun eventStreamEvent(): Any?
}