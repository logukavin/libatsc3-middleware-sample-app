package com.nextgenbroadcast.mobile.middleware.sample.lifecycle

import android.app.Application
import android.text.Html
import androidx.lifecycle.*
import com.nextgenbroadcast.mobile.core.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.core.presentation.IUserAgentPresenter
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.sample.R

class ReceiverViewModel(
        application: Application,
        private val presenter: IReceiverPresenter,
        private val agentPresenter: IUserAgentPresenter,
        private val playerPresenter: IMediaPlayerPresenter
) : AndroidViewModel(application) {
    private val _appDataLog = MediatorLiveData<CharSequence>()

    val appDataLog: LiveData<CharSequence> = _appDataLog
    val stateDescription = presenter.receiverState.asLiveData().map { receiverState ->
        when(receiverState.state) {
            ReceiverState.State.IDLE -> {
                application.getString(R.string.receiver_status_idle)
            }
            ReceiverState.State.SCANNING -> {
                val num = receiverState.configCount - receiverState.configIndex
                application.getString(R.string.receiver_status_scanning, num, receiverState.configCount)
            }
            ReceiverState.State.TUNING -> {
                application.getString(R.string.receiver_status_tuning)
            }
            else -> ""
        }
    }

    init {
        _appDataLog.addSource(agentPresenter.appData.asLiveData()) { data ->
            _appDataLog.value = formatLog(data, playerPresenter.rmpMediaUri.value?.toString())
        }
        _appDataLog.addSource(playerPresenter.rmpMediaUri.asLiveData()) { uri ->
            _appDataLog.value = formatLog(agentPresenter.appData.value, uri?.toString())
        }
    }

    fun getFrequency(): Int {
        return presenter.freqKhz.value ?: 0
    }

    fun tune(freqKhz: Int) {
        presenter.tune(PhyFrequency.user(listOf(freqKhz)))
    }

    private fun formatLog(data: AppData?, rpmMediaUri: String?): CharSequence {
        val contextId = data?.appContextId ?: "<b>NO Context ID</b>"
        val entryPoint = data?.appEntryPage ?: "<b>NO Entry Point</b>"
        val cachePath = data?.cachePath ?: "<b>NO Application available</b>"
        val mediaUrl = rpmMediaUri ?: "<b>NO Media Url</b>"
        return Html.fromHtml("> $contextId<br>> $entryPoint<br>> $cachePath<br>> $mediaUrl", Html.FROM_HTML_MODE_LEGACY)
    }
}