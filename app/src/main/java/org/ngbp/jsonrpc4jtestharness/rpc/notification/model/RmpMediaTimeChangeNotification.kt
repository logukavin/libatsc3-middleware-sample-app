package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType

data class RmpMediaTimeChangeNotification(
        var msgType: String = NotificationType.RMP_MEDIA_TIME_CHANGE.value,
        var currentTime: Double? = null
): RPCNotification