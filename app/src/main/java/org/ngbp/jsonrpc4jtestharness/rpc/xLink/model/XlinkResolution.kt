package org.ngbp.jsonrpc4jtestharness.rpc.xLink.model

data class XlinkResolution (
    var xlink: String? = null,
    var disposition: Disposition? = null,
    var timing: Timing? = null
)

data class Timing (
    var currentPosition: Double? = null,
    var periodStart: String? = null,
    var duration: Double? = null
)

data class Disposition (
    var code: Int? = null,
    var description: String? = null
)
