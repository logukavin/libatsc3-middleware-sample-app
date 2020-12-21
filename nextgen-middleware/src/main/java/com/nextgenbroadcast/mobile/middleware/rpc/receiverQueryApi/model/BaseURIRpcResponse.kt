package com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model

import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse

data class BaseURIRpcResponse(
        var baseURI: String? = null
) : RpcResponse()