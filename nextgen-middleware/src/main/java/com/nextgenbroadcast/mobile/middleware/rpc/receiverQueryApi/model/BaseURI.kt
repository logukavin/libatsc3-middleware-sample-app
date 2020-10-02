package com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model

import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse

data class BaseURI(
        var baseURI: String? = null
) : RpcResponse()