package com.nextgenbroadcast.mobile.middleware.scoreboard

import androidx.lifecycle.*
import com.nextgenbroadcast.mobile.middleware.dev.telemetry.entity.ClientTelemetryEvent
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TelemetryDevice
import kotlinx.coroutines.flow.Flow

class SharedViewModel : ViewModel() {
    private val _deviceList: MutableLiveData<List<TelemetryDevice>> = MutableLiveData(emptyList())
    private val _chartDevices = MutableLiveData<List<String>>(emptyList())
    private val _deviceFlowMap = MutableLiveData<Map<String, Flow<ClientTelemetryEvent>>>(emptyMap())

    val devicesToAdd = _chartDevices.mapWith(_deviceFlowMap) { (devices, deviceToFlow) ->
        devices?.subtract(deviceToFlow?.keys ?: emptyList())
    }.distinctUntilChanged()

    val devicesToRemove = _chartDevices.mapWith(_deviceFlowMap) { (devices, deviceFlows) ->
        deviceFlows?.keys?.subtract(devices ?: emptyList())
    }.distinctUntilChanged()

    val deviceIdList: LiveData<List<String>> = _deviceList.map { list ->
        list.map { device -> device.id }
    }

    val chartDevices = _chartDevices.mapWith(_deviceList) { (chartList, deviceList) ->
        deviceList?.filter { chartList?.contains(it.id) ?: false }
    }

    val selectedDeviceId: MutableLiveData<String?> = MutableLiveData()

    fun setDeviceSelection(deviceId: String?) {
        selectedDeviceId.value = deviceId
    }

    fun setDevicesList(deviceIds: List<TelemetryDevice>) {
        _deviceList.value = deviceIds
    }

    fun addDeviceChart(deviceId: String) {
        val list = _chartDevices.value?.toMutableList() ?: mutableListOf()
        list.add(deviceId)
        _chartDevices.value = list
    }

    fun removeDeviceChart(deviceId: String) {
        val list = _chartDevices.value?.toMutableList() ?: return
        list.remove(deviceId)
        _chartDevices.value = list
        synchronizeChartSelection(deviceId)
    }

    private fun synchronizeChartSelection(deviceId: String) {
        if (selectedDeviceId.value == deviceId) {
            selectedDeviceId.value = null
        }
    }

    fun addFlow(deviceId: String, flow: Flow<ClientTelemetryEvent>) {
        val map = _deviceFlowMap.value?.toMutableMap() ?: mutableMapOf()
        map[deviceId] = flow
        _deviceFlowMap.postValue(map)
    }

    fun removeFlow(deviceId: String) {
        val map = _deviceFlowMap.value?.toMutableMap() ?: return
        map.remove(deviceId)
        _deviceFlowMap.postValue(map)
    }

    fun getDeviceFlow(deviceId: String): Flow<ClientTelemetryEvent>? {
        return _deviceFlowMap.value?.get(deviceId)
    }

}