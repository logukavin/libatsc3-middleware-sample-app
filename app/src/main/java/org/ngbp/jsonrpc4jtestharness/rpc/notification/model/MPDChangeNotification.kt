package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType

data class MPDChangeNotification(
        var msgType: String = NotificationType.MPD_CHANGE.value
): RPCNotification