package com.nextgenbroadcast.mobile.middleware.rpc.notification.model

import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.AlertingRpcResponse

data class AlertingChangeNotification (
    var alertList: List<AlertingRpcResponse.Alert?>? = null
): RPCNotification(NotificationType.ALERT_CHANGE)