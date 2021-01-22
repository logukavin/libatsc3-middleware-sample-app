package com.nextgenbroadcast.mobile.middleware.sample.lifecycle

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ViewViewModel : ViewModel() {
    val showDebugInfo = MutableLiveData(true)
    val showPhyInfo = MutableLiveData(false)

    val debugData = MutableLiveData<String>()
}