package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class Alerting(
        var alertList: List<Alert>? = null
) : RpcResponse()

data class Alert(
        var alertingType: String? = null,
        var alertingFragment: String? = null,
        var receiveTime: String? = null,
        var filteredEventList: MutableList<FilteredEventList?>? = null
)

data class FilteredEventList(
        var aeaId: String? = null
)
