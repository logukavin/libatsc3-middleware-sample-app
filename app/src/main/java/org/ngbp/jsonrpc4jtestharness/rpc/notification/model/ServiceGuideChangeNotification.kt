package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Urls

data class ServiceGuideChangeNotification (
        var urlList: List<Urls?>? = null
): RPCNotification(NotificationType.SERVICE_GUIDE_CHANGE)