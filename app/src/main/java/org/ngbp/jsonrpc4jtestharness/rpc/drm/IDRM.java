package org.ngbp.jsonrpc4jtestharness.rpc.drm;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rpc.drm.model.DRMOperation;
import org.ngbp.jsonrpc4jtestharness.rpc.drm.model.NotifyParams;

@JsonRpcService("")
public interface IDRM {
    @JsonRpcMethod("org.atsc.notify")
    NotifyParams drmNotification();

    @JsonRpcMethod("org.atsc.drmOperation")
    DRMOperation drmOperation();

}
