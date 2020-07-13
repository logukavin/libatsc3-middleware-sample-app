package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class AudioAccessibilityPref(
        var videoDescriptionService: VideoDescriptionService? = null,
        var audioEIService: AudioEIService? = null
) : RpcResponse()

data class VideoDescriptionService(
        var enabled: Boolean = false,
        var language: String? = null
)

data class AudioEIService(
        var enabled: Boolean = false,
        var language: String? = null
)
