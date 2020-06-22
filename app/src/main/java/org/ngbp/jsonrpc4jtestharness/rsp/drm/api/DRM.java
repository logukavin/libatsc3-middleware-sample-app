package org.ngbp.jsonrpc4jtestharness.rsp.drm.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rsp.drm.model.DRMOperation;
import org.ngbp.jsonrpc4jtestharness.rsp.drm.model.NotifyParams;
import org.ngbp.jsonrpc4jtestharness.rsp.mapper.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rsp.queryDeviceInf.model.DeviceInfoParams;

@JsonRpcService("")
public interface DRM {
    @JsonRpcMethod("org.atsc.notify")
    JsonRpcResponse<NotifyParams> drmNotification();

    @JsonRpcMethod("org.atsc.drmOperation")
    JsonRpcResponse<DRMOperation> drmOperation();

}
