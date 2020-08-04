package com.nextgenbroadcast.mobile.middleware.rpc.filterCodes

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse
import com.nextgenbroadcast.mobile.middleware.rpc.filterCodes.model.GetFilterCodes

@JsonRpcType
interface IFilterCodes {
    @JsonRpcMethod("org.atsc.getFilterCodes")
    fun getFilterCodes(): GetFilterCodes

    @JsonRpcMethod("org.atsc.setFilterCodes")
    fun setFilterCodes(): RpcResponse
}