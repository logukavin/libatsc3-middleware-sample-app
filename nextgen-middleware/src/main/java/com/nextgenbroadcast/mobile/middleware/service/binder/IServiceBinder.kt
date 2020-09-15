package com.nextgenbroadcast.mobile.middleware.service.binder

import com.nextgenbroadcast.mobile.middleware.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IUserAgentPresenter

interface IServiceBinder {
    val receiverPresenter: IReceiverPresenter
    val selectorPresenter: ISelectorPresenter
    val userAgentPresenter: IUserAgentPresenter
    val mediaPlayerPresenter: IMediaPlayerPresenter
}