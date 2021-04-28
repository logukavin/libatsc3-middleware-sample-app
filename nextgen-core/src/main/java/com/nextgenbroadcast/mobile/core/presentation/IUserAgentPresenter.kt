package com.nextgenbroadcast.mobile.core.presentation

import com.nextgenbroadcast.mobile.core.model.AppData
import kotlinx.coroutines.flow.StateFlow

@Deprecated("Use the ReceiverContentProvider instead")
interface IUserAgentPresenter {
    val appData: StateFlow<AppData?>
    val appState: StateFlow<ApplicationState>

    fun setApplicationState(state: ApplicationState)

    fun getWebServerCertificateHash(): String?
}