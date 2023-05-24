package com.nextgenbroadcast.mobile.middleware.phy

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import com.nextgenbroadcast.mobile.middleware.service.startAtsc3ServiceForDevice

class Atsc3DeviceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.action?.let { action ->
            if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                val device: UsbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) ?: return@let
                DeviceUtils.dumpDevice(TAG, device, action)
                startAtsc3ServiceForDevice(this, device, true)
            } else if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                Log.d(TAG, "onCreate::action: BluetoothAdapter.ACTION_STATE_CHANGED: ")
            }
        }

        finish()
    }

    companion object {
        val TAG: String = Atsc3DeviceActivity::class.java.simpleName
    }
}