package com.nextgenbroadcast.mobile.core.presentation

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.serviceGuide.SGScheduleMap

interface ISelectorPresenter {
    val sltServices: LiveData<List<AVService>>
    val selectedService: LiveData<AVService?>

    val schedule: LiveData<SGScheduleMap>

    fun selectService(service: AVService)
}