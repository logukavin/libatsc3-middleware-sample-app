package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model

data class Alerting (
    var alertingType: String? = null,
    var alertingFragment: String? = null,
    var receiveTime: String? = null,
    var filteredEventList: MutableList<FilteredEventList?>? = null
)

data class FilteredEventList (
    var aeaId: String? = null
)
