package org.ngbp.jsonrpc4jtestharness.rpc.keys.model

data class NotifyParams (
    var msgType: String? = null,
    var timeout: MutableList<Timeout?>? = null
)

data class Timeout (
    var key: String? = null,
    var time: Int? = null
)
