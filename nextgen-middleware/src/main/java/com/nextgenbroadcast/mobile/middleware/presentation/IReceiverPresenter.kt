package com.nextgenbroadcast.mobile.middleware.presentation

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.ReceiverState

interface IReceiverPresenter {
    val receiverState: LiveData<ReceiverState>

    fun openRoute(pcapFile: String): Boolean
    fun openRoute(device: UsbDevice, manager: UsbManager): Boolean
    fun stopRoute()
    fun closeRoute()
}