package org.ngbp.jsonrpc4jtestharness.rpc.drm.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class DRMOperation(
        var message: MutableList<Any?>? = null
) : RpcResponse()