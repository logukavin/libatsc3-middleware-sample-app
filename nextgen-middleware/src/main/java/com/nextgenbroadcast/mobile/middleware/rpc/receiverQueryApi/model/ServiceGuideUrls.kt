package com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model

import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse

data class ServiceGuideUrls(
        var urlList: List<Urls>? = null
) : RpcResponse()

data class Urls(
        var sgType: String? = null,
        var sgUrl: String? = null
)