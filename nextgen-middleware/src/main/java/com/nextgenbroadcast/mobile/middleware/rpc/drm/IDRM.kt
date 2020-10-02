package com.nextgenbroadcast.mobile.middleware.rpc.drm

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import com.nextgenbroadcast.mobile.middleware.rpc.drm.model.DRMOperation

@JsonRpcType
interface IDRM {
    @JsonRpcMethod("org.atsc.drmOperation")
    fun drmOperation(): DRMOperation
}