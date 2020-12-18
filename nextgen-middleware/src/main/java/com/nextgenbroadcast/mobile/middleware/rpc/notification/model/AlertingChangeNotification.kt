package com.nextgenbroadcast.mobile.middleware.rpc.notification.model

import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.AlertingRpcResponse

data class AlertingChangeNotification (
    var msgType: String? = null,
    var alertList: MutableList<AlertingRpcResponse?>? = null
)