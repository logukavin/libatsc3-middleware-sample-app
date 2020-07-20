package org.ngbp.jsonrpc4jtestharness.rpc.xLink

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

@JsonRpcType
interface IXLink {
    @JsonRpcMethod("org.atsc.xlinkResolution")
    fun xLinkResolved(): RpcResponse
}