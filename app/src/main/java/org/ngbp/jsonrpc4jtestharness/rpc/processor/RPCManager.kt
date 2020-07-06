package org.ngbp.jsonrpc4jtestharness.rpc.processor

import org.ngbp.jsonrpc4jtestharness.rpc.KeyCode
import org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.model.DeviceInfo
import org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.model.DeviceInput
import org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.model.Info

class RPCManager {
    val deviceInfoParams = DeviceInfo()
        get() {
            return field.apply {
                deviceMake = "Acme"
                deviceModel = "A300"
                deviceInput = DeviceInput().apply {
                    this.ArrowUp = KeyCode.ARROW_UP
                    this.ArrowDown = KeyCode.ARROW_DOWN
                    this.ArrowRight = KeyCode.ARROW_RIGHT
                    this.ArrowLeft = KeyCode.ARROW_LEFT
                    this.Select = KeyCode.SELECT
                    this.Back = KeyCode.BACK
                }
                deviceInfo = Info().apply {
                    this.numberOfTuners = 1
                    this.yearOfMfr = 2017
                }
            }
        }
    private var callback: ReceiverActionCallback? = null

    fun getCallback(): ReceiverActionCallback? {
        return callback
    }

    fun setCallback(callback: ReceiverActionCallback?) {
        this.callback = callback
    }

    fun updateViewPosition(scaleFactor: Double?, xPos: Double?, yPos: Double?) {
        callback?.updateViewPosition(scaleFactor ?: 1.0, xPos ?: 0.0, yPos ?: 0.0)
    }

    companion object {
        private val instance: RPCManager? = null
    }


}