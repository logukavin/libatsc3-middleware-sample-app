package org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class RmpWallClockTime(
        var wallClock: String? = null
) : RpcResponse()