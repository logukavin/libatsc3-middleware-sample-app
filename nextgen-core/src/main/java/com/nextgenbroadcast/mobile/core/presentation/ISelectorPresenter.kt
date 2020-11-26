package com.nextgenbroadcast.mobile.core.presentation

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.core.serviceGuide.SGScheduleMap

interface ISelectorPresenter {
    val sltServices: LiveData<List<SLSService>>
    val selectedService: LiveData<SLSService?>

    val schedule: LiveData<SGScheduleMap>

    fun selectService(service: SLSService)
}