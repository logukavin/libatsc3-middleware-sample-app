package org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf

import org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.model.DeviceInfo
import org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.model.DeviceInfoParams
import org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.model.DeviceInput
import org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.model.Info

class QueryDeviceInfoImpl : IQueryDeviceInfo {
    override fun queryDeviceInfo(deviceInfoParams: List<String>): DeviceInfo? {
        val deviceInfoParams = DeviceInfo()
        with(deviceInfoParams) {
            deviceMake = "Acme"
            deviceModel = "A300"
            deviceInput = DeviceInput().apply {
                this.ArrowUp = 38
                this.ArrowDown = 40
                this.ArrowRight = 39
                this.ArrowLeft = 37
                this.Select = 13
                this.Back = 461
            }
            deviceInfo = Info().apply {
                this.numberOfTuners = 1
                this.yearOfMfr = 2017
            }
        }

        return deviceInfoParams
    }
}
