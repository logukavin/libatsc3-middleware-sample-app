package com.nextgenbroadcast.mobile.middleware.sample.lifecycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.nextgenbroadcast.mobile.core.presentation.IUserAgentPresenter
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.presentation.ApplicationState

class UserAgentViewModel(
        private val presenter: IUserAgentPresenter
) : ViewModel() {

    val isReady = presenter.appData.asLiveData().map { isAppReady(it) }
    val appData = presenter.appData.asLiveData()

    private fun isAppReady(appData: AppData?): Boolean {
        return appData?.isAvailable() ?: false
    }

    fun setApplicationState(state: ApplicationState) {
        presenter.setApplicationState(state)
    }

    fun getServerCertificateHash(): String? {
        return presenter.getWebServerCertificateHash()
    }
}