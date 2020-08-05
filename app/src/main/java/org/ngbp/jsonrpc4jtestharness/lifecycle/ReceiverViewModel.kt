package org.ngbp.jsonrpc4jtestharness.lifecycle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.nextgenbroadcast.mobile.middleware.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IUserAgentPresenter
import com.nextgenbroadcast.mobile.core.model.AppData

class ReceiverViewModel(
        private val agentPresenter: IUserAgentPresenter,
        private val playerPresenter: IMediaPlayerPresenter
) : ViewModel() {
    private val _appDataLog = MediatorLiveData<String>()

    val appDataLog: LiveData<String> = _appDataLog

    init {
        _appDataLog.addSource(agentPresenter.appData) { data ->
            _appDataLog.value = formatLog(data, playerPresenter.rmpMediaUrl.value)
        }
        _appDataLog.addSource(playerPresenter.rmpMediaUrl) { url ->
            _appDataLog.value = formatLog(agentPresenter.appData.value, url)
        }
    }

    private fun formatLog(data: AppData?, rpmMediaUri: String?): String {
        val contextId = data?.appContextId ?: "NO Context ID"
        val entryPoint = data?.appEntryPage ?: "NO Entry Point"
        val cachePath = data?.cachePath ?: "NO Application available"
        val mediaUrl = rpmMediaUri ?: "NO Media Url"
        return "> $contextId\n> $entryPoint\n> $cachePath\n> $mediaUrl"
    }
}