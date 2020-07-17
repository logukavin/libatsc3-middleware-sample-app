package org.ngbp.jsonrpc4jtestharness.lifecycle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import org.ngbp.jsonrpc4jtestharness.controller.IMediaPlayerController
import org.ngbp.jsonrpc4jtestharness.controller.IUserAgentController
import org.ngbp.jsonrpc4jtestharness.controller.model.AppData

class ReceiverViewModel(
        private val agentController: IUserAgentController,
        private val mediaController: IMediaPlayerController
) : ViewModel() {
    private val _appDataLog = MediatorLiveData<String>()

    val appDataLog: LiveData<String> = _appDataLog

    init {
        _appDataLog.addSource(agentController.appData) { data ->
            _appDataLog.value = formatLog(data, mediaController.rmpMediaUrl.value)
        }
        _appDataLog.addSource(mediaController.rmpMediaUrl) { url ->
            _appDataLog.value = formatLog(agentController.appData.value, url)
        }
    }

    private fun formatLog(data: AppData?, rpmMediaUri: String?): String {
        val contextId = data?.appContextId ?: "NO Context ID"
        val entryPoint = data?.appEntryPage ?: "NO Entry Point"
        val mediaUrl = rpmMediaUri ?: "NO Media Url"
        return "> $contextId\n> $entryPoint\n> $mediaUrl"
    }
}