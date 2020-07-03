package org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model

data class ContentRecoveryState (
    var audioWatermark: Int? = null,
    var videoWatermark: Int? = null,
    var audioFingerprint: Int? = null,
    var videoFingerprint: Int? = null
)