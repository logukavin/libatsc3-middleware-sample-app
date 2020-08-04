package com.nextgenbroadcast.mobile.middleware.controller.service

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.Urls

interface IServiceController : IReceiverPresenter, ISelectorPresenter {
    val serviceGuidUrls: LiveData<List<Urls>?>
}