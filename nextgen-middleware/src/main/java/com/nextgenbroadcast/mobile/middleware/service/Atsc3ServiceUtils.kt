package com.nextgenbroadcast.mobile.middleware.service

import android.content.Context
import android.hardware.usb.UsbDevice
import com.nextgenbroadcast.mobile.middleware.DeviceTypeSelectionDialog
import com.nextgenbroadcast.mobile.middleware.atsc3.source.Atsc3Source
import com.nextgenbroadcast.mobile.middleware.atsc3.source.UsbAtsc3Source

fun startAtsc3ServiceForDevice(context: Context, device: UsbDevice) {
    if (UsbAtsc3Source.isSaankhyaFX3PrebootDevice(device)) {
        context.startActivity(DeviceTypeSelectionDialog.newIntent(context, device))
    } else if(UsbAtsc3Source.getSaankhyaFX3BootedDeviceType(device) != Atsc3Source.DEVICE_TYPE_UNKNOWN) {
        Atsc3ForegroundService.startForDevice(context, device, UsbAtsc3Source.getSaankhyaFX3BootedDeviceType(device))
    } else {
        Atsc3ForegroundService.startForDevice(context, device, Atsc3Source.DEVICE_TYPE_AUTO)
    }
}