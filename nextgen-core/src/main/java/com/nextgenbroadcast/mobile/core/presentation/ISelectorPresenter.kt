package com.nextgenbroadcast.mobile.core.presentation

import com.nextgenbroadcast.mobile.core.model.AVService
import kotlinx.coroutines.flow.StateFlow

@Deprecated("Use media session instead")
interface ISelectorPresenter {
    val sltServices: StateFlow<List<AVService>>
    val selectedService: StateFlow<AVService?>

    fun selectService(service: AVService): Boolean
}