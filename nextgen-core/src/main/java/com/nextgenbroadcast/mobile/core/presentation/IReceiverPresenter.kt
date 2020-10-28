package com.nextgenbroadcast.mobile.core.presentation

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.ReceiverState

interface IReceiverPresenter {
    val receiverState: LiveData<ReceiverState>
    val freqKhz: LiveData<Int>

    fun openRoute(path: String): Boolean
    fun closeRoute()

    fun createMMTSource(): Any

    fun tune(freqKhz: Int)
}