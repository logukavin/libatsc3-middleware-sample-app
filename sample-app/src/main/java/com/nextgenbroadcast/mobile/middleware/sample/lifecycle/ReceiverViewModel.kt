package com.nextgenbroadcast.mobile.middleware.sample.lifecycle

import android.text.Html
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
    private val _appDataLog = MediatorLiveData<CharSequence>()

    val appDataLog: LiveData<CharSequence> = _appDataLog

    init {
        _appDataLog.addSource(agentPresenter.appData) { data ->
            _appDataLog.value = formatLog(data, playerPresenter.rmpMediaUrl.value)
        }
        _appDataLog.addSource(playerPresenter.rmpMediaUrl) { url ->
            _appDataLog.value = formatLog(agentPresenter.appData.value, url)
        }
    }

    private fun formatLog(data: AppData?, rpmMediaUri: String?): CharSequence {
        val contextId = data?.appContextId ?: "<b>NO Context ID</b>"
        val entryPoint = data?.appEntryPage ?: "<b>NO Entry Point</b>"
        val cachePath = data?.cachePath ?: "<b>NO Application available</b>"
        val mediaUrl = rpmMediaUri ?: "<b>NO Media Url</b>"
        return Html.fromHtml("> $contextId<br>> $entryPoint<br>> $cachePath<br>> $mediaUrl", Html.FROM_HTML_MODE_LEGACY)
    }
}