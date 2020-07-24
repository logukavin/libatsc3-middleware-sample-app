package org.ngbp.jsonrpc4jtestharness.lifecycle

import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.ngbp.jsonrpc4jtestharness.presentation.IUserAgentPresenter
import org.ngbp.jsonrpc4jtestharness.core.model.AppData

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