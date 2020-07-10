package org.ngbp.jsonrpc4jtestharness.rpc.keys.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class Keys(
        var accepted: List<String>? = null
) : RpcResponse()