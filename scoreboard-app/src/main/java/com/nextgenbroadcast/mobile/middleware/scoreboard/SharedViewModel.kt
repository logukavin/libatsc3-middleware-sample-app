package com.nextgenbroadcast.mobile.middleware.scoreboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.ClientTelemetryEvent
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TelemetryDevice
import kotlinx.coroutines.flow.Flow

class SharedViewModel : ViewModel() {

    private var _deviceIdList: MutableLiveData<List<String>> = MutableLiveData(emptyList())
    var deviceIdList: LiveData<List<String>> = _deviceIdList

    private var _connectedDeviceList: MutableLiveData<List<TelemetryDevice>> = MutableLiveData()
    var connectedDeviceList: LiveData<List<TelemetryDevice>> = _connectedDeviceList

    private var _addDeviceToChartEvent: MutableLiveData<String> = MutableLiveData()
    var addDeviceToChartEvent: LiveData<String> = _addDeviceToChartEvent

    private var _removeDeviceToChartEvent: MutableLiveData<String> = MutableLiveData()
    var removeDeviceToChartEvent: LiveData<String> = _removeDeviceToChartEvent

    var deviceFlowMap = mutableMapOf<String, Flow<ClientTelemetryEvent>?>()

    fun addDevicesIdList(deviceIds: List<String>) {
        _deviceIdList.value = deviceIds
    }

    fun addDeviceToChartList(deviceId: String) {
        _addDeviceToChartEvent.value = deviceId
    }

    fun removeDeviceFromChartList(deviceId: String) {
        _removeDeviceToChartEvent.value = deviceId
    }

    fun updateConnectedDevices(deviceList: List<TelemetryDevice>) {
        _connectedDeviceList.value = deviceList
    }

    fun getFlow(deviceId: String): Flow<ClientTelemetryEvent>? {
        return deviceFlowMap[deviceId]
    }
}