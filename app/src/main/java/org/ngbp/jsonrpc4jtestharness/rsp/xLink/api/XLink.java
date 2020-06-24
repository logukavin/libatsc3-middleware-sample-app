package org.ngbp.jsonrpc4jtestharness.rsp.xLink.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rsp.drm.model.NotifyParams;

@JsonRpcService("")
public interface XLink {
    @JsonRpcMethod("org.atsc.notify")
    Object xLinkResolutionNotification();

    @JsonRpcMethod("org.atsc.xlinkResolution")
    NotifyParams xLinkResolved();
}
