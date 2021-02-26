package com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model

import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse

data class AlertingRpcResponse(
        var alertList: List<Alert>? = null
) : RpcResponse() {

    data class Alert(
            var alertingType: String? = null,
            var alertingFragment: String? = null,
            var receiveTime: String? = null,
            var filteredEventList: MutableList<FilteredEventList?>? = null
    ) {
        data class FilteredEventList(
                var aeaId: String? = null
        )
    }
}