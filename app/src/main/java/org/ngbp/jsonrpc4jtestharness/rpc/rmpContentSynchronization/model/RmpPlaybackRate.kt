package org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class RmpPlaybackRate(
        var playbackRate: Int? = null
) : RpcResponse()