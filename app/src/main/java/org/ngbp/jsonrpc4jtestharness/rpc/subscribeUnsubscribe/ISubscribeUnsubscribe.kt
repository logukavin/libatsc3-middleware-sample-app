package org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcParam
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.EmptyModel
import org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe.model.Subscribe

@JsonRpcType
interface ISubscribeUnsubscribe {
    @JsonRpcMethod("org.atsc.subscribe")
    fun integratedSubscribe(@JsonRpcParam("msgType") types: List<String>): EmptyModel

    @JsonRpcMethod("org.atsc.unsubscribe")
    fun integratedUnsubscribe(): Subscribe?
}