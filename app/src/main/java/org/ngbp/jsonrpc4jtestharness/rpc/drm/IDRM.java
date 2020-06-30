package org.ngbp.jsonrpc4jtestharness.rpc.drm;

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;

import org.ngbp.jsonrpc4jtestharness.rpc.drm.model.DRMOperation;
import org.ngbp.jsonrpc4jtestharness.rpc.drm.model.NotifyParams;

@JsonRpcType
public interface IDRM {
    @JsonRpcMethod("org.atsc.notify")
    NotifyParams drmNotification();

    @JsonRpcMethod("org.atsc.drmOperation")
    DRMOperation drmOperation();

}
