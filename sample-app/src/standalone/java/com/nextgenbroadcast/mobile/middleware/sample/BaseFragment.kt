package com.nextgenbroadcast.mobile.middleware.sample

import androidx.fragment.app.Fragment
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.factory.UserAgentViewModelFactory
import com.nextgenbroadcast.mobile.middleware.sample.view.ReceiverPlayerView
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService

open class BaseFragment : Fragment() {
    open fun onBind(binder: IServiceBinder) {}

    open fun onUnbind() {}

    fun preparePlayerView(playerView: ReceiverPlayerView) {}
}