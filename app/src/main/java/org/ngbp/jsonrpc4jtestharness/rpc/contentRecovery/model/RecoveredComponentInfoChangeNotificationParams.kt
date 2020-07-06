package org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model

data class RecoveredComponentInfoChangeNotificationParams (
    var msgType: String? = null,
    var mediaType: String? = null,
    var componentID: String? = null,
    var descriptor: String? = null
)