package com.nextgenbroadcast.mobile.middleware

import com.nextgenbroadcast.mobile.middleware.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IUserAgentPresenter

interface IServiceBinder {
    fun getReceiverPresenter(): IReceiverPresenter
    fun getSelectorPresenter(): ISelectorPresenter
    fun getUserAgentPresenter(): IUserAgentPresenter
    fun getMediaPlayerPresenter(): IMediaPlayerPresenter
}