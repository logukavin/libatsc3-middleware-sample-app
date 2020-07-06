package org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.model

data class GetFilterCodes (
    var filters: MutableList<Filters?>? = null
)

data class Filters (
    var filterCode: Int? = null,
    var expires: String? = null
)
