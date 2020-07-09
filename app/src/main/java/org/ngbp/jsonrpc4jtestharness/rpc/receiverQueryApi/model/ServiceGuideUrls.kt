package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class ServiceGuideUrls(
        var urlList: List<Urls>? = null
) : RpcResponse()

data class Urls(
        var sgType: String? = null,
        var sgUrl: String? = null
)