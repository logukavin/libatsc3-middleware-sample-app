package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType

data class ServiceChangeNotification (
        var msgType: String = NotificationType.SERVICE_CHANGE.value,
        var service: String? = null
): RPCNotification