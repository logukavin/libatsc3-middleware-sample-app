package org.ngbp.jsonrpc4jtestharness.rpc.xLink

import org.ngbp.jsonrpc4jtestharness.rpc.drm.model.NotifyParams

class XLinkImpl : IXLink {
    override fun xLinkResolutionNotification(): Any? {
        return null
    }

    override fun xLinkResolved(): NotifyParams? {
        return null
    }
}