package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class ServiceGuideUrls(
        var urlList: List<Urls>? = null
) : RpcResponse()

data class Urls(
        var sgType: String? = null,
        var sgUrl: String? = null
)

enum class ServiceGuideType(val value: String) {
    SERVICE("Service"),
    SCHEDULE("Schedule"),
    CONTENT("Content")
}