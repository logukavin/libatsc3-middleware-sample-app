package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Urls

data class ServiceGuideChangeNotification (
        var msgType: String = NotificationType.SERVICE_GUIDE_CHANGE.value,
        var urlList: List<Urls?>? = null
): BaseNotification()