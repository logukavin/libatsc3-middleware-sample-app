package org.ngbp.jsonrpc4jtestharness.rpc.drm.model

data class DRMOperationParam (
    var systemId: String? = null,
    var service: String? = null,
    var message: MutableList<Message?>? = null
)

data class Message (
    var operation: String? = null,
    var licenseUri: String? = null
)
