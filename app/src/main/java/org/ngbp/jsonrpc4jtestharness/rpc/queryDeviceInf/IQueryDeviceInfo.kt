package org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.model.DeviceInfoParams

@JsonRpcType
interface IQueryDeviceInfo {
    @JsonRpcMethod("org.atsc.query.deviceInfo")
    fun queryDeviceInfo(): DeviceInfoParams?
}