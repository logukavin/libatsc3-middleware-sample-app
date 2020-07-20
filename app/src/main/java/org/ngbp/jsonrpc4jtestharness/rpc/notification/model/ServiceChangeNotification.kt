package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

data class ServiceChangeNotification (
    var msgType: String? = null,
    var service: String? = null
)