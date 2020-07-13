package org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class GetFilterCodes(
        var filters: MutableList<Filters?>? = null
) : RpcResponse()

data class Filters(
        var filterCode: Int? = null,
        var expires: String? = null
)
