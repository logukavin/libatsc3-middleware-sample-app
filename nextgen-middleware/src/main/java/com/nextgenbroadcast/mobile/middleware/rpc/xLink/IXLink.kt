package com.nextgenbroadcast.mobile.middleware.rpc.xLink

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse

@JsonRpcType
interface IXLink {
    @JsonRpcMethod("org.atsc.xlinkResolution")
    fun xLinkResolved(): RpcResponse
}