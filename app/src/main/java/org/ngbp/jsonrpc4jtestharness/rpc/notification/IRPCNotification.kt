package org.ngbp.jsonrpc4jtestharness.rpc.notification

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcParam
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

@JsonRpcType
interface IRPCNotification {
    @JsonRpcMethod("org.atsc.notify")
    fun rMPNotification(@JsonRpcParam("msgType") msgType: String, @JsonRpcParam("playbackState") playbackState: Int): RpcResponse
}