package com.nextgenbroadcast.mobile.core.presentation

import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.ApplicationState
import kotlinx.coroutines.flow.StateFlow

@Deprecated("Use the ReceiverContentProvider instead")
interface IUserAgentPresenter {
    val appData: StateFlow<AppData?>

    fun setApplicationState(state: ApplicationState)

    fun getWebServerCertificateHash(): String?
}