package org.ngbp.jsonrpc4jtestharness.rpc.keys

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcParam
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse
import org.ngbp.jsonrpc4jtestharness.rpc.keys.model.Keys

@JsonRpcType
interface IKeys {
    @JsonRpcMethod("org.atsc.request.keys")
    fun requestKeys(@JsonRpcParam("keys") listOfKeys: List<String>): Keys

    @JsonRpcMethod("org.atsc.relinquish.keys")
    fun relinquishKeys(@JsonRpcParam("keys") listOfKeys: List<String>): RpcResponse

    @JsonRpcMethod("org.atsc.notify")
    fun requestKeysTimeout(): RpcResponse
}