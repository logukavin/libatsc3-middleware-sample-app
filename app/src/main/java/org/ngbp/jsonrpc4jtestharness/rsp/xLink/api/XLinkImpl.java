package org.ngbp.jsonrpc4jtestharness.rsp.xLink.api;

import org.ngbp.jsonrpc4jtestharness.rsp.drm.model.NotifyParams;
import org.ngbp.jsonrpc4jtestharness.rsp.mapper.JsonRpcResponse;

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
