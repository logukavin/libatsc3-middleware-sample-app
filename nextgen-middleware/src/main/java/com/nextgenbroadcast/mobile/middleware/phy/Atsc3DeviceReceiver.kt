package com.nextgenbroadcast.mobile.middleware.phy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService

class Atsc3DeviceReceiver(
        private val deviceName: String
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        intent?.action?.let { action ->
            val device: UsbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) ?: return

            DeviceUtils.dumpDevice(TAG, device, action)

            if (deviceName != device.deviceName) return

            if (action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                Atsc3ForegroundService.stopForDevice(context, device)
            }
        }
    }

    companion object {
        val TAG: String = Atsc3DeviceReceiver::class.java.simpleName
    }
}