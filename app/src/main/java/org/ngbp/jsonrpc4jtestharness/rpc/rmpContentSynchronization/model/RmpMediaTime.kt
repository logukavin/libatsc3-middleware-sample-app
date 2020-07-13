package org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class RmpMediaTime(
        var currentTime: String? = null,
        var startDate: String? = null
) : RpcResponse()