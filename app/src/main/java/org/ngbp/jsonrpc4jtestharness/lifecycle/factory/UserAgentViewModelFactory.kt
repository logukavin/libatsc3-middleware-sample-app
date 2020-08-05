package org.ngbp.jsonrpc4jtestharness.lifecycle.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nextgenbroadcast.mobile.middleware.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IUserAgentPresenter
import org.ngbp.jsonrpc4jtestharness.lifecycle.RMPViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.ReceiverViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.SelectorViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.UserAgentViewModel
import javax.inject.Inject

class UserAgentViewModelFactory @Inject constructor(
        private val agentPresenter: IUserAgentPresenter,
        private val playerPresenter: IMediaPlayerPresenter,
        private val selectorPresenter: ISelectorPresenter
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RMPViewModel::class.java)) {
            return RMPViewModel(playerPresenter) as T
        } else if (modelClass.isAssignableFrom(UserAgentViewModel::class.java)) {
            return UserAgentViewModel(agentPresenter) as T
        } else if (modelClass.isAssignableFrom(ReceiverViewModel::class.java)) {
            return ReceiverViewModel(agentPresenter, playerPresenter) as T
        } else if (modelClass.isAssignableFrom(SelectorViewModel::class.java)) {
            return SelectorViewModel(selectorPresenter) as T
        }

        throw ClassNotFoundException()
    }

}