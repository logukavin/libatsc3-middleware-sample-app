package com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model

import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse

data class CC(
        var ccEnabled: Boolean = false
) : RpcResponse()