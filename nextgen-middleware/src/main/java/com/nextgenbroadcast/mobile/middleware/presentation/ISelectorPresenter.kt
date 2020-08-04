package com.nextgenbroadcast.mobile.middleware.presentation

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.SLSService

interface ISelectorPresenter {
    val sltServices: LiveData<List<SLSService>>
    val selectedService: LiveData<SLSService?>

    fun selectService(service: SLSService)
}