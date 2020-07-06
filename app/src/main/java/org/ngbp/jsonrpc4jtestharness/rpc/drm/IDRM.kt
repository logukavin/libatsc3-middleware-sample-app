package org.ngbp.jsonrpc4jtestharness.rpc.drm

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.drm.model.DRMOperation
import org.ngbp.jsonrpc4jtestharness.rpc.drm.model.NotifyParams

@JsonRpcType
interface IDRM {
    @JsonRpcMethod("org.atsc.notify")
    fun drmNotification(): NotifyParams?

    @JsonRpcMethod("org.atsc.drmOperation")
    fun drmOperation(): DRMOperation?
}