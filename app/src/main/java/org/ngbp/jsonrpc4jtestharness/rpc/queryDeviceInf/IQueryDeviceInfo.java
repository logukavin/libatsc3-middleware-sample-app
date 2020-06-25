package org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.model.DeviceInfoParams;

@JsonRpcService("")
public interface IQueryDeviceInfo {
    @JsonRpcMethod("org.atsc.query.deviceInfo")
    DeviceInfoParams queryDeviceInfo();
}
