package com.nextgenbroadcast.mobile.middleware.rpc.notification.model

import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.Urls

data class ServiceGuideChangeNotification (
        var urlList: List<Urls?>? = null
): RPCNotification(NotificationType.SERVICE_GUIDE_CHANGE)