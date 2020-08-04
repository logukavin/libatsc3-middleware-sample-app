package com.nextgenbroadcast.mobile.middleware.rpc.drm.model

import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse

data class DRMOperation(
        var message: MutableList<Any?>? = null
) : RpcResponse()