package org.ngbp.jsonrpc4jtestharness.lifecycle.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.ngbp.jsonrpc4jtestharness.controller.IReceiverController
import org.ngbp.jsonrpc4jtestharness.lifecycle.RMPViewModel
import org.ngbp.jsonrpc4jtestharness.lifecycle.UserAgentViewModel
import javax.inject.Inject

class UserAgentViewModelFactory @Inject constructor(
        private val appController: IReceiverController
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RMPViewModel::class.java)) {
            return RMPViewModel(appController) as T
        } else if (modelClass.isAssignableFrom(UserAgentViewModel::class.java)) {
            return UserAgentViewModel(appController) as T
        }

        throw ClassNotFoundException()
    }

}