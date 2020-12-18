package com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model

import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse

data class CCRpcResponse(
        var ccEnabled: Boolean = false
) : RpcResponse()