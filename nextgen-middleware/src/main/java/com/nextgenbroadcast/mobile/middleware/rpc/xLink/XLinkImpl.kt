package com.nextgenbroadcast.mobile.middleware.rpc.xLink

import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse

class XLinkImpl : IXLink {
    override fun xLinkResolved(): RpcResponse {
        return RpcResponse()
    }
}