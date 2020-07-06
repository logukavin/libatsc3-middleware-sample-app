package org.ngbp.jsonrpc4jtestharness.rpc.xLink

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.drm.model.NotifyParams

@JsonRpcType
interface IXLink {
    @JsonRpcMethod("org.atsc.notify")
    fun xLinkResolutionNotification(): Any?

    @JsonRpcMethod("org.atsc.xlinkResolution")
    fun xLinkResolved(): NotifyParams?
}