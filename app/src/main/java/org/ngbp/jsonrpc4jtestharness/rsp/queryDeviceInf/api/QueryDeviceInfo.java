package org.ngbp.jsonrpc4jtestharness.rsp.queryDeviceInf.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rsp.mapper.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rsp.queryDeviceInf.model.DeviceInfoParams;

@JsonRpcService("")
public interface QueryDeviceInfo {
    @JsonRpcMethod("org.atsc.query.deviceInfo")
    JsonRpcResponse<DeviceInfoParams> queryDeviceInfo();
}
