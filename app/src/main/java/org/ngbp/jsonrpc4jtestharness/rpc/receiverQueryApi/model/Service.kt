package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class Service(
        var service: String? = null
) : RpcResponse()