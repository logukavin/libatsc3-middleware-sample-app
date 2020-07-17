package org.ngbp.jsonrpc4jtestharness.lifecycle.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ngbp.jsonrpc4jtestharness.controller.IMediaPlayerController
import org.ngbp.jsonrpc4jtestharness.controller.IUserAgentController
import org.ngbp.jsonrpc4jtestharness.lifecycle.RMPViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.ReceiverViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.UserAgentViewModel
import javax.inject.Inject

class UserAgentViewModelFactory @Inject constructor(
        private val userAgentController: IUserAgentController,
        private val mediaPlayerController: IMediaPlayerController
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RMPViewModel::class.java)) {
            return RMPViewModel(mediaPlayerController) as T
        } else if (modelClass.isAssignableFrom(UserAgentViewModel::class.java)) {
            return UserAgentViewModel(userAgentController) as T
        } else if (modelClass.isAssignableFrom(ReceiverViewModel::class.java)) {
            return ReceiverViewModel(userAgentController, mediaPlayerController) as T
        }

        throw ClassNotFoundException()
    }

}