package com.nextgenbroadcast.mobile.middleware.sample.lifecycle

import android.app.Application
import android.net.Uri
import android.text.Html
import android.text.Spanned
import androidx.lifecycle.*
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.LocationFrequencyType
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.reader.SensorFrequencyType
import com.nextgenbroadcast.mobile.middleware.sample.R
import com.nextgenbroadcast.mobile.middleware.sample.core.mapWith

class ViewViewModel(
    application: Application
) : AndroidViewModel(application) {
    val sources = MutableLiveData<List<Pair<String, String>>>(emptyList())
    val services = MutableLiveData<List<AVService>>(emptyList())
    val currentServiceTitle = MutableLiveData<String>()
    val isPlaying = MutableLiveData<Boolean>()
    val mediaUri = MutableLiveData<Uri?>()
    val appData = MutableLiveData<AppData>(null)
    val receiverState = MutableLiveData(ReceiverState.idle())

    val appDataLog = appData.mapWith(mediaUri) { (data, uri) ->
        formatLog(data, uri?.toString())
    }

    val stateDescription = receiverState.map { receiverState ->
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
    val isCancelable = receiverState.map { receiverState ->
        receiverState.state == ReceiverState.State.SCANNING
    }

    val showDebugInfo = MutableLiveData<Boolean>()
    val showPhyInfo = MutableLiveData<Boolean>()
    val showPhyChart = MutableLiveData<Boolean>()

    val debugData = MutableLiveData<CharSequence>()

    val defaultService = services.distinctUntilChanged().map { list ->
        list.firstOrNull()
    }

    // must be cleared on unBind
    val enableTelemetry = MutableLiveData(false)
    val sensorTelemetryEnabled = MutableLiveData(true)
    val sensorFrequencyType = MutableLiveData(SensorFrequencyType.MEDIUM)
    val locationTelemetryEnabled = MutableLiveData(true)
    val locationFrequencyType = MutableLiveData(LocationFrequencyType.MEDIUM)

    fun clearSubscriptions(owner: LifecycleOwner) {
        enableTelemetry.removeObservers(owner)
        sensorTelemetryEnabled.removeObservers(owner)
        sensorFrequencyType.removeObservers(owner)
        locationTelemetryEnabled.removeObservers(owner)
        locationFrequencyType.removeObservers(owner)
    }

    private fun formatLog(data: AppData?, rpmMediaUri: String?): CharSequence {
        val contextId = data?.appContextId ?: "<b>NO Context ID</b>"
        val entryPoint = data?.appEntryPage ?: "<b>NO Entry Point</b>"
        val cachePath = data?.cachePath ?: "<b>NO Application available</b>"
        val mediaUrl = rpmMediaUri ?: "<b>NO Media Url</b>"
        return Html.fromHtml("> $contextId<br>> $entryPoint<br>> $cachePath<br>> $mediaUrl", Html.FROM_HTML_MODE_LEGACY)
    }
}