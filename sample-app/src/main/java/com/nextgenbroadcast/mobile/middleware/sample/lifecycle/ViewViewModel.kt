package com.nextgenbroadcast.mobile.middleware.sample.lifecycle

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nextgenbroadcast.mobile.core.model.AVService

class ViewViewModel : ViewModel() {
    val sources = MutableLiveData<List<Pair<String, String>>>(emptyList())
    val services = MutableLiveData<List<AVService>>(emptyList())
    val currentServiceTitle = MutableLiveData<String>()
    val isPlaying = MutableLiveData<Boolean>()

    val showDebugInfo = MutableLiveData(true)
    val showPhyInfo = MutableLiveData(false)

    val debugData = MutableLiveData<String>()
}