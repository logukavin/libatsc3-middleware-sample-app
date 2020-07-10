package org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class CecoveredComponentInfo(
        var component: MutableList<Component?>? = null
) : RpcResponse()

data class Component(
        var mediaType: String? = null,
        var componentID: String? = null,
        var descriptor: String? = null
)