package com.nextgenbroadcast.mobile.middleware.controller.service

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.Urls

interface IServiceController : IReceiverPresenter, ISelectorPresenter {
    val serviceGuidUrls: LiveData<List<Urls>?>

    fun openRoute(device: UsbDevice): Boolean
    fun closeRoute(device: UsbDevice): Boolean
    fun stopRoute()
}