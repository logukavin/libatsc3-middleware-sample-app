package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

data class ContentChangeNotification (
    var msgType: String? = null,
    var packageList: MutableList<String?>? = null
)