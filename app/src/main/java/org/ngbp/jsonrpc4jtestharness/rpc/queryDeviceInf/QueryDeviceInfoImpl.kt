package org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf

import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCManager
import org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.model.DeviceInfo
import org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.model.DeviceInput

class QueryDeviceInfoImpl(val rpcManager: RPCManager) : IQueryDeviceInfo {
    override fun queryDeviceInfo(deviceInfoParams: List<String>): DeviceInfo? {
        return rpcManager.deviceInfoParams
    }
}
