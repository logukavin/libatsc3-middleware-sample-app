package com.nextgenbroadcast.mobile.middleware.sample.lifecycle

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.middleware.telemetry.reader.LocationFrequencyType
import com.nextgenbroadcast.mobile.middleware.telemetry.reader.SensorFrequencyType

class ViewViewModel : ViewModel() {
    val sources = MutableLiveData<List<Pair<String, String>>>(emptyList())
    val services = MutableLiveData<List<AVService>>(emptyList())
    val currentServiceTitle = MutableLiveData<String>()
    val isPlaying = MutableLiveData<Boolean>()

    val showDebugInfo = MutableLiveData(true)
    val showPhyInfo = MutableLiveData(false)

    val enableCollectTelemetry = MutableLiveData(false)
    val collectSensorTelemetry = MutableLiveData(true)
    val sensorFrequencyType = MutableLiveData(SensorFrequencyType.MEDIUM)
    val collectLocationTelemetry = MutableLiveData(true)
    val locationFrequencyType = MutableLiveData(LocationFrequencyType.MEDIUM)
    val debugData = MutableLiveData<String>()
}