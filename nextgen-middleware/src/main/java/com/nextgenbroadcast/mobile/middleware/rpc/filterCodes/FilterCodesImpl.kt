package com.nextgenbroadcast.mobile.middleware.rpc.filterCodes

import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse
import com.nextgenbroadcast.mobile.middleware.rpc.filterCodes.model.Filters
import com.nextgenbroadcast.mobile.middleware.rpc.filterCodes.model.GetFilterCodes
import java.util.*

class FilterCodesImpl : IFilterCodes {
    override fun getFilterCodes(): GetFilterCodes {
        val filters = Filters().apply {
            expires = " 10"
            filterCode = 10
        }
        val filtersList = ArrayList<Filters?>()
        filtersList.add(filters)
        val getFilterCodes = GetFilterCodes()
        getFilterCodes.filters = filtersList
        return getFilterCodes
    }

    override fun setFilterCodes(): RpcResponse {
        return RpcResponse()
    }
}