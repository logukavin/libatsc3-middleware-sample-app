package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType

data class RmpMediaTimeChangeNotification(
        var currentTime: Double
): RPCNotification(NotificationType.RMP_MEDIA_TIME_CHANGE)