package org.ngbp.jsonrpc4jtestharness.rpc.drm.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class NotifyParams(
        var msgType: String? = null,
        var systemId: String? = null,
        var message: MutableList<Any?>? = null
) : RpcResponse()