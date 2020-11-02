package com.nextgenbroadcast.mobile.middleware.sample.lifecycle

import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.nextgenbroadcast.mobile.core.presentation.IUserAgentPresenter
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.presentation.ApplicationState

class UserAgentViewModel(
        private val presenter: IUserAgentPresenter
) : ViewModel() {

    val isReady = Transformations.map(presenter.appData) { isAppReady(it) }
    val appData = Transformations.distinctUntilChanged(presenter.appData)

    private fun isAppReady(appData: AppData?): Boolean {
        return appData?.isAvailable() ?: false
    }

    fun setApplicationState(state: ApplicationState) {
        presenter.setApplicationState(state)
    }
}