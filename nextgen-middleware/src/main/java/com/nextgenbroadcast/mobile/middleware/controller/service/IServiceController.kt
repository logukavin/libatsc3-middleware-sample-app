package com.nextgenbroadcast.mobile.middleware.controller.service

import android.hardware.usb.UsbDevice
import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl

interface IServiceController : IReceiverPresenter, ISelectorPresenter {
    val serviceGuidUrls: LiveData<List<SGUrl>?>

    fun openRoute(device: UsbDevice): Boolean
    fun stopRoute()
}