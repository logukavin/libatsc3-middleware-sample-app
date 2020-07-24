package org.ngbp.jsonrpc4jtestharness.presentation

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.core.model.AppData

interface IUserAgentPresenter {
    val appData: LiveData<AppData?>
}