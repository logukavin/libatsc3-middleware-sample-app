package com.nextgenbroadcast.mobile.middleware.phy

import android.app.Activity
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import com.nextgenbroadcast.mobile.middleware.Atsc3ForegroundService

class Atsc3DeviceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.action?.let { action ->
            val device: UsbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) ?: return

            DeviceUtils.dumpDevice(TAG, device, action)

            if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                Atsc3ForegroundService.startForDevice(this, device)
            }
        }

        finish()
    }

    companion object {
        val TAG: String = Atsc3DeviceActivity::class.java.simpleName
    }
}