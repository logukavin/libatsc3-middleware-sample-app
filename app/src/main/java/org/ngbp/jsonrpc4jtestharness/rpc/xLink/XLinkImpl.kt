package org.ngbp.jsonrpc4jtestharness.rpc.xLink

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse
import org.ngbp.jsonrpc4jtestharness.rpc.drm.model.NotifyParams

class XLinkImpl : IXLink {
    override fun xLinkResolutionNotification(): RpcResponse {
        return RpcResponse()
    }

    override fun xLinkResolved(): NotifyParams {
        return NotifyParams()
    }
}