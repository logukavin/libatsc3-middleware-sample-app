package com.nextgenbroadcast.mobile.middleware.phy

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService

internal class Atsc3UsbPhyConnector {

    fun connect(context: Context, compatibleList: List<Pair<Int, Int>>): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = usbManager.deviceList.map { (_, device) ->
            device
        }

        devices.firstOrNull { device ->
            compatibleList.firstOrNull { (vendor, product) ->
                device.vendorId == vendor && device.productId == product
            } != null
        }?.let { device ->
            if (usbManager.hasPermission(device)) {
                // open device using a new Intent to start Service as foreground
                Atsc3ForegroundService.startForDevice(context, device)
            } else {
                val intent = Intent(context, Atsc3ForegroundService.clazz).apply {
                    action = Atsc3ForegroundService.ACTION_USB_PERMISSION
                }
                usbManager.requestPermission(device, PendingIntent.getService(context, 0, intent, 0))
            }
        }

        return true
    }

}