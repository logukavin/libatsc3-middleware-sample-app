package org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class AudioVolume(
        var audioVolume: Double? = null
) : RpcResponse()