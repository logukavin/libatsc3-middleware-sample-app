package com.nextgenbroadcast.mobile.middleware.controller.service

import android.hardware.usb.UsbDevice
import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.core.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.Urls

interface IServiceController : IReceiverPresenter, ISelectorPresenter {
    val serviceGuidUrls: LiveData<List<Urls>?>

    fun openRoute(device: UsbDevice): Boolean
    fun stopRoute()
}