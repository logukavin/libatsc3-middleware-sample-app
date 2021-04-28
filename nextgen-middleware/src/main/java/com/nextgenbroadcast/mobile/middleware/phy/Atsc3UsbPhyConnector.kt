package com.nextgenbroadcast.mobile.middleware.phy

import android.content.Context
import android.hardware.usb.UsbManager
import com.nextgenbroadcast.mobile.middleware.service.startAtsc3ServiceForDevice

internal class Atsc3UsbPhyConnector {

    fun connect(context: Context, compatibleList: List<Pair<Int, Int>>): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        usbManager.deviceList.values.firstOrNull { device ->
            compatibleList.firstOrNull { (vendor, product) ->
                device.vendorId == vendor && device.productId == product
            } != null
        }?.let { device ->
            // open device using a new Intent to start Service as foreground
            startAtsc3ServiceForDevice(context, device)
        }

        return true
    }

}