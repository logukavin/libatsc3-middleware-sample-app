package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType

data class ServiceChangeNotification (
        var service: String? = null
): RPCNotification(NotificationType.SERVICE_CHANGE)