package org.ngbp.jsonrpc4jtestharness.rpc.asynchronousNotificationsofChanges.model

data class ContentChangeNotification (
    var msgType: String? = null,
    var packageList: MutableList<String?>? = null
)