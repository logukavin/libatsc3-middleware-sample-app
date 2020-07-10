package org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class ContentRecoveryState(
        var audioWatermark: Int? = null,
        var videoWatermark: Int? = null,
        var audioFingerprint: Int? = null,
        var videoFingerprint: Int? = null
) : RpcResponse()