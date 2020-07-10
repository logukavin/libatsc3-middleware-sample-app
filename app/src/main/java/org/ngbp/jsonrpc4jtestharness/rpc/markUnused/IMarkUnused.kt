package org.ngbp.jsonrpc4jtestharness.rpc.markUnused

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

@JsonRpcType
interface IMarkUnused {
    @JsonRpcMethod("org.atsc.cache.markUnused")
    fun markUnused(): RpcResponse
}