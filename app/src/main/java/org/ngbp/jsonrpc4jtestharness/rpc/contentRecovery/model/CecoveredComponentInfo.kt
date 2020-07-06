package org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model

data class CecoveredComponentInfo (
    var component: MutableList<Component?>? = null
)

data class Component (
    var mediaType: String? = null,
    var componentID: String? = null,
    var descriptor: String? = null
)