package org.ngbp.jsonrpc4jtestharness.lifecycle

import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.nextgenbroadcast.mobile.middleware.presentation.IUserAgentPresenter
import com.nextgenbroadcast.mobile.core.model.AppData

class UserAgentViewModel(
        private val presenter: IUserAgentPresenter
) : ViewModel() {

    val isReady = Transformations.map(presenter.appData) { isAppReady(it ) }
    val appData = Transformations.distinctUntilChanged(presenter.appData)

    private fun isAppReady(appData: AppData?): Boolean {
        return appData?.let {
            !it.appContextId.isNullOrEmpty() && !it.appEntryPage.isNullOrEmpty()
        } ?: false
    }
}