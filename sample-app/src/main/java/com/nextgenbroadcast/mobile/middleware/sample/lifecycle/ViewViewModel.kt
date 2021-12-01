package com.nextgenbroadcast.mobile.middleware.sample.lifecycle

import android.app.Application
import android.net.NetworkCapabilities
import android.net.Uri
import android.text.Html
import androidx.lifecycle.*
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.model.bCastEntryPageUrlFull
import com.nextgenbroadcast.mobile.middleware.sample.MobileInternetDetector.CellularNetworkState
import com.nextgenbroadcast.mobile.middleware.sample.R
import com.nextgenbroadcast.mobile.middleware.sample.asString
import com.nextgenbroadcast.mobile.middleware.sample.core.mapWith
import com.nextgenbroadcast.mobile.middleware.sample.model.LogInfo
import com.nextgenbroadcast.mobile.middleware.sample.model.LogInfo.Group
import com.nextgenbroadcast.mobile.middleware.sample.model.LogInfo.Record
import com.nextgenbroadcast.mobile.middleware.sample.settings.SensorState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

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
    val cellularState = MutableLiveData<CellularNetworkState>()
    val networkCapabilitiesState = MutableLiveData<NetworkCapabilities>()

    private val cellularInfo = cellularState.mapWith(networkCapabilitiesState) { (cellular, capabilities) ->
        application.getString(
            R.string.debug_cellular_info,
            "<b>${cellular?.asString() ?: "-"}</b>",
            "<b>${capabilities?.linkUpstreamBandwidthKbps ?: ""}</b>",
            "<b>${capabilities?.linkDownstreamBandwidthKbps ?: ""}</b>"
        )
    }

    val appDataLog = appData.mapWith(mediaUri, cellularInfo) { (data, uri, cellularInfo) ->
        formatLog(data, uri?.toString(), cellularInfo)
    }

    val stateDescription = receiverState.map { receiverState ->
        when (receiverState.state) {
            ReceiverState.State.IDLE -> {
                application.getString(R.string.receiver_status_idle)
            }
            ReceiverState.State.SCANNING -> {
                val num = receiverState.configCount - receiverState.configIndex
                application.getString(
                    R.string.receiver_status_scanning,
                    num,
                    receiverState.configCount
                )
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
    val showMediaInfo = MutableLiveData<Boolean>()

    val debugData = MutableLiveData<CharSequence>()

    val defaultService = services.distinctUntilChanged().map { list ->
        list.firstOrNull { it.default } ?: list.firstOrNull()
    }

    // must be cleared on unBind
    val enableTelemetry = MutableLiveData(false)
    val sensorTelemetryEnabled = MutableLiveData(true)
    val logsInfo = MutableLiveData<Map<String, Boolean>>()
    val logChangingChannel = Channel<Pair<String, Boolean>>()
    val sensorsEnableList = MutableLiveData<Map<String, Boolean>>(mapOf())
    val sensorsFrequencyList = MutableLiveData<Map<String, Long>>()
    val eventSensorEnableChannel = Channel<Pair<String, Boolean>>()
    val eventSensorFrequencyChannel = Channel<Pair<String, Long>>()
    val sensorLiveData = sensorsEnableList.mapWith(sensorsFrequencyList){ (sensorEnable, sensorDelay) ->
        sensorEnable?.map {
            SensorState(it.key, it.value, sensorDelay?.get(it.key))
        }
    }

    val groupedLogsInfo: LiveData<List<LogInfo>>
        get() = logsInfo.map(::groupLogs)

    private var _mediaDataInfo = MutableLiveData<String>()
    val mediaDataInfo = _mediaDataInfo.distinctUntilChanged()

    private fun groupLogs(map: Map<String, Boolean>): List<LogInfo> {
        val records = map.map { (key, enabled) ->
            Record(
                name = key,
                displayName = formatLogName(key),
                enabled = enabled
            )
        }
        val single = mutableMapOf<String, MutableList<Record>>()

        return records.groupBy {
            extractPrefixOrNull(name = it.name)
        }.flatMap { (groupName, records) ->
            if (records.size > 1) {
                groupName?.let {
                    listOf(Group(it)) + records
                } ?: records
            } else {
                groupName?.let { name ->
                    val singleGroupName = name.split(" ").first()
                    single[singleGroupName]?.addAll(records) ?: let {
                        single[singleGroupName] = records.toMutableList()
                    }
                }
                emptyList()
            }
        }.toMutableList().apply {
            single.forEach { (groupName, records) ->
                val firstIndex = indexOfFirst { it is Group && it.title.startsWith(groupName) }
                if (firstIndex == -1) {
                    addAll(listOf(Group(groupName)) + records)
                } else {
                    addAll(firstIndex, listOf(Group(groupName)) + records)
                }
            }
        }
    }

    private fun extractPrefixOrNull(name: String): String? {
        return name.split("_")
            .filter { it.isNotBlank() }
            .run {
                getOrNull(1)?.let {
                    first() + " " + it
                } ?: firstOrNull()
            }
    }

    private fun formatLogName(name: String): String {
        return name.replace("_", " ").trim()
    }

    fun clearSubscriptions(owner: LifecycleOwner) {
        enableTelemetry.removeObservers(owner)
        sensorTelemetryEnabled.removeObservers(owner)
        logsInfo.removeObservers(owner)
        sensorsEnableList.removeObservers(owner)
        sensorsFrequencyList.removeObservers(owner)
        sensorLiveData.removeObservers(owner)
    }

    fun changeLogFlagStatus(name: String, enabled: Boolean) = viewModelScope.launch {
        logChangingChannel.send(name to enabled)
    }

    private fun formatLog(data: AppData?, rpmMediaUri: String?, cellularInfo: String?): CharSequence {
        val contextId = data?.contextId?.let { id -> "<b>Context ID:</b> $id" } ?: "<b>NO Context ID</b>"
        val entryPoint = data?.let {
            listOfNotNull(
                data.bBandEntryPageUrl?.let { url -> "<b>BB:</b> $url" },
                data.bCastEntryPageUrlFull?.let { url -> "<b>BC:</b> $url" }
            ).takeIf { it.isNotEmpty() }
                ?.joinToString(prefix = "<b>App </b>")
        } ?: "<b>NO Entry Point</b>"
        val cachePath = data?.cachePath?.let { path -> "<b>App Path:</b> $path" }
            ?: "<b>NO Application available</b>"
        val mediaUrl = rpmMediaUri?.let { uri -> "<b>Media:</b> $uri" } ?: "<b>NO Media Url</b>"
        val cellular = cellularInfo ?: "<b>NO Cellular data</b>"
        return Html.fromHtml(
            "> $contextId<br>> $entryPoint<br>> $cachePath<br>> $mediaUrl", // <br>> $cellular
            Html.FROM_HTML_MODE_LEGACY
        )
    }

    fun sendEventSensorChangeFrequency(sensorFrequencyState: Pair<String, Long>) {
        viewModelScope.launch {
            eventSensorFrequencyChannel.send(sensorFrequencyState)
        }
    }

    fun sendEventSensorChangeEnableState(sensorEnableState: Pair<String, Boolean>) {
        viewModelScope.launch {
            eventSensorEnableChannel.send(sensorEnableState)
        }
    }

    fun updateMediaInfo(mediaInfo: String) {
        _mediaDataInfo.value = mediaInfo
    }
}