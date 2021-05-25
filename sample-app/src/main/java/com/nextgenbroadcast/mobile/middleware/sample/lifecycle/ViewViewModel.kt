package com.nextgenbroadcast.mobile.middleware.sample.lifecycle

import androidx.lifecycle.LifecycleOwner
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

    val showDebugInfo = MutableLiveData<Boolean>()
    val showPhyInfo = MutableLiveData<Boolean>()

    val debugData = MutableLiveData<String>()

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
}