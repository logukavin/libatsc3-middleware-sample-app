package org.ngbp.jsonrpc4jtestharness.rsp.xLink.api;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rsp.drm.model.NotifyParams;

public class XLinkImpl implements XLink {
    @Override
    public JsonRpcResponse<Object> xLinkResolutionNotification() {
        return null;
    }

    @Override
    public JsonRpcResponse<NotifyParams> xLinkResolved() {
        return null;
    }
}
