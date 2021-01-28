package com.nextgenbroadcast.mobile.core.presentation

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.AVService

interface ISelectorPresenter {
    val sltServices: LiveData<List<AVService>>
    val selectedService: LiveData<AVService?>

    fun selectService(service: AVService): Boolean
}