package org.ngbp.jsonrpc4jtestharness.rsp.drm.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rsp.drm.model.DRMOperation;
import org.ngbp.jsonrpc4jtestharness.rsp.drm.model.NotifyParams;

@JsonRpcService("")
public interface DRM {
    @JsonRpcMethod("org.atsc.notify")
    JsonRpcResponse<NotifyParams> drmNotification();

    @JsonRpcMethod("org.atsc.drmOperation")
    JsonRpcResponse<DRMOperation> drmOperation();

}
