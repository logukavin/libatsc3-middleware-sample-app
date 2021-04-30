package com.nextgenbroadcast.mobile.middleware.service

import android.content.Context
import android.hardware.usb.UsbDevice
import com.nextgenbroadcast.mobile.middleware.DeviceTypeSelectionDialog
import com.nextgenbroadcast.mobile.middleware.atsc3.source.Atsc3Source
import com.nextgenbroadcast.mobile.middleware.atsc3.source.UsbAtsc3Source

fun startAtsc3ServiceForDevice(context: Context, device: UsbDevice) {
    val type = UsbAtsc3Source.getSaankhyaFX3DeviceType(device)
    when {
        type == Atsc3Source.DEVICE_TYPE_PREBOOT -> {
            context.startActivity(DeviceTypeSelectionDialog.newIntent(context, device))
        }
        type != Atsc3Source.DEVICE_TYPE_UNKNOWN -> {
            Atsc3ForegroundService.startForDevice(context, device, type)
        }
        else -> {
            Atsc3ForegroundService.startForDevice(context, device, Atsc3Source.DEVICE_TYPE_AUTO)
        }
    }
}