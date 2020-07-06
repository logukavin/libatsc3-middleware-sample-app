package org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.model

data class setRMPURLParams (
    var operation: String? = null,
    var rmpurl: String? = null,
    var rmpSyncTime: Int? = null
)