package com.nextgenbroadcast.mobile.middleware.sample.lifecycle

import android.app.Application
import android.net.Uri
import android.text.Html
import androidx.lifecycle.*
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.core.presentation.IUserAgentPresenter
import com.nextgenbroadcast.mobile.middleware.sample.R
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Deprecated("Use the ReceiverContentResolver instead")
class ReceiverViewModel(
        application: Application,
        private val presenter: IReceiverPresenter,
        private val agentPresenter: IUserAgentPresenter,
        private val playerPresenter: IMediaPlayerPresenter
) : AndroidViewModel(application) {
    private val _appDataLog = MediatorLiveData<CharSequence>()
    private val _mediaUri = MutableLiveData<Uri?>()

    val appData = MutableLiveData<AppData>(null)
    val receiverState = MutableLiveData(ReceiverState.idle())

    val appDataLog: LiveData<CharSequence> = _appDataLog
    val stateDescription = /*presenter.receiverState.asLiveData()*/receiverState.map { receiverState ->
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
        _appDataLog.addSource(/*agentPresenter.appData.asLiveData()*/appData) { data ->
            _appDataLog.value = formatLog(data, /*playerPresenter.rmpMediaUri.value?.toString()*/_mediaUri.value?.toString())
        }
        _appDataLog.addSource(/*playerPresenter.rmpMediaUri.asLiveData()*/_mediaUri) { uri ->
            _appDataLog.value = formatLog(/*agentPresenter.appData.value*/appData.value, uri?.toString())
        }

        // we must collect data in viewModelScope instead of asLiveData() because VM life cycle is different from Fragment ones
        viewModelScope.launch {
            playerPresenter.rmpMediaUri.collect {
                _mediaUri.value = it
            }
        }
    }

    fun getFrequency(): Int {
//        return presenter.freqKhz.value
        return 0
    }

    fun tune(freqKhz: Int) {
//        presenter.tune(PhyFrequency.user(listOf(freqKhz)))
    }

    private fun formatLog(data: AppData?, rpmMediaUri: String?): CharSequence {
        val contextId = data?.appContextId ?: "<b>NO Context ID</b>"
        val entryPoint = data?.appEntryPage ?: "<b>NO Entry Point</b>"
        val cachePath = data?.cachePath ?: "<b>NO Application available</b>"
        val mediaUrl = rpmMediaUri ?: "<b>NO Media Url</b>"
        return Html.fromHtml("> $contextId<br>> $entryPoint<br>> $cachePath<br>> $mediaUrl", Html.FROM_HTML_MODE_LEGACY)
    }
}