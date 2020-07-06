package org.ngbp.jsonrpc4jtestharness.rpc.filterCodes

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.model.GetFilterCodes

@JsonRpcType
interface IFilterCodes {
    @JsonRpcMethod("org.atsc.getFilterCodes")
    fun getFilterCodes(): GetFilterCodes?

    @JsonRpcMethod("org.atsc.setFilterCodes")
    fun setFilterCodes(): Any?
}