package org.ngbp.jsonrpc4jtestharness.rpc.xLink;

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;

import org.ngbp.jsonrpc4jtestharness.rpc.drm.model.NotifyParams;

@JsonRpcType
public interface IXLink {
    @JsonRpcMethod("org.atsc.notify")
    Object xLinkResolutionNotification();

    @JsonRpcMethod("org.atsc.xlinkResolution")
    NotifyParams xLinkResolved();
}
