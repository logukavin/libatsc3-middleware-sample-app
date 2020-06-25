package org.ngbp.jsonrpc4jtestharness.rpc.xLink;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rpc.drm.model.NotifyParams;

@JsonRpcService("")
public interface IXLink {
    @JsonRpcMethod("org.atsc.notify")
    Object xLinkResolutionNotification();

    @JsonRpcMethod("org.atsc.xlinkResolution")
    NotifyParams xLinkResolved();
}
