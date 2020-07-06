package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model

data class CaptionDisplay (
    var msgType: String? = null,
    var cta708: cta708? = null,
    var imsc1: imsc1? = null
)

data class cta708 (
    var characterColor: String? = null,
    var characterOpacity: Double = 0.0,
    var characterSize: Int = 0,
    var fontStyle: String? = null,
    var backgroundColor: String? = null,
    var backgroundOpacity: Int = 0,
    var characterEdge: String? = null,
    var characterEdgeColor: String? = null,
    var windowColor: String? = null,
    var windowOpacity: Int = 0
)

data class imsc1 (
    var region_textAlign: String? = null,
    var content_fontWeight: String? = null
)
