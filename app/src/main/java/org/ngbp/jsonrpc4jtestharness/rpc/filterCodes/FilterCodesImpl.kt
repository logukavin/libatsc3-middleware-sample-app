package org.ngbp.jsonrpc4jtestharness.rpc.filterCodes

import org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.model.Filters
import org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.model.GetFilterCodes
import java.util.*

class FilterCodesImpl : IFilterCodes {
    override fun getFilterCodes(): GetFilterCodes? {
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

    override fun setFilterCodes(): Any? {
        return null
    }
}