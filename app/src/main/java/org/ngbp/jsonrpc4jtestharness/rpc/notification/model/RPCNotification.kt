package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType

open class RPCNotification(notificationType: NotificationType) {
    private var msgType: String = notificationType.value
}