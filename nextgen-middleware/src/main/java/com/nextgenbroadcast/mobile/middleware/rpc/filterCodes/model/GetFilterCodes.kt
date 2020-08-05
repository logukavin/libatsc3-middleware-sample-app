package com.nextgenbroadcast.mobile.middleware.rpc.filterCodes.model

import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse

data class GetFilterCodes(
        var filters: MutableList<Filters?>? = null
) : RpcResponse()

data class Filters(
        var filterCode: Int? = null,
        var expires: String? = null
)
