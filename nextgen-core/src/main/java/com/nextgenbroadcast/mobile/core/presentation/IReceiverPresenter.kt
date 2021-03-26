package com.nextgenbroadcast.mobile.core.presentation

import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import kotlinx.coroutines.flow.StateFlow

interface IReceiverPresenter {
    val receiverState: StateFlow<ReceiverState>
    val freqKhz: StateFlow<Int>

    fun openRoute(path: String): Boolean
    fun closeRoute()

    fun tune(frequency: PhyFrequency)
}