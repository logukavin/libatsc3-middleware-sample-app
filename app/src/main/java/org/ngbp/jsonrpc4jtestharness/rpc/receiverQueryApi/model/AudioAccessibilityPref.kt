package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model

data class AudioAccessibilityPref (
    var videoDescriptionService: VideoDescriptionService? = null,
    var audioEIService: AudioEIService? = null
)

data class VideoDescriptionService (
    var enabled: Boolean = false,
    var language: String? = null
)

data class AudioEIService (
    var enabled: Boolean = false,
    var language: String? = null
)
