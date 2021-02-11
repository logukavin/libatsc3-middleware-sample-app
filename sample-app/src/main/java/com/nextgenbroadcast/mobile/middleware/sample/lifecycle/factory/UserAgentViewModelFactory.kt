package com.nextgenbroadcast.mobile.middleware.sample.lifecycle.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nextgenbroadcast.mobile.core.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.core.presentation.IUserAgentPresenter
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.RMPViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.ReceiverViewModel
import com.nextgenbroadcast.mobile.middleware.sample.lifecycle.UserAgentViewModel
import javax.inject.Inject

class UserAgentViewModelFactory @Inject constructor(
        private val agentPresenter: IUserAgentPresenter,
        private val playerPresenter: IMediaPlayerPresenter,
        private val receiverPresenter: IReceiverPresenter
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RMPViewModel::class.java)) {
            return RMPViewModel(playerPresenter) as T
        } else if (modelClass.isAssignableFrom(UserAgentViewModel::class.java)) {
            return UserAgentViewModel(agentPresenter) as T
        } else if (modelClass.isAssignableFrom(ReceiverViewModel::class.java)) {
            return ReceiverViewModel(receiverPresenter, agentPresenter, playerPresenter) as T
        }

        throw ClassNotFoundException()
    }

}