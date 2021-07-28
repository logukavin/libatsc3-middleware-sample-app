package com.nextgenbroadcast.mobile.middleware.scoreboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TelemetryDevice
import com.nextgenbroadcast.mobile.middleware.scoreboard.telemetry.TelemetryManager

class SharedViewModel : ViewModel() {
    var telemetryManager: TelemetryManager? = null

    private var _deviceIdList: MutableLiveData<List<String>> = MutableLiveData(emptyList())
    var deviceIdList: LiveData<List<String>> = _deviceIdList

    private var _connectedDeviceList: MutableLiveData<List<TelemetryDevice>> = MutableLiveData()
    var connectedDeviceList: LiveData<List<TelemetryDevice>> = _connectedDeviceList

    fun addDevicesIdList(deviceIds: List<String>) {
        _deviceIdList.value = deviceIds
    }

    fun addDeviceToChartList(deviceId: String) {
        telemetryManager?.connectDevice(deviceId)
        updateConnectedDevices()
    }

    fun removeDeviceFromChartList(deviceId: String) {
        telemetryManager?.disconnectDevice(deviceId)
        updateConnectedDevices()
    }

    private fun updateConnectedDevices() {
        _connectedDeviceList.value = telemetryManager?.getConnectedDevices()
    }
}