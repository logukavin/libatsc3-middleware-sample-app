package com.nextgenbroadcast.mobile.middleware.presentation

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.ReceiverState

interface IReceiverPresenter {
    val receiverState: LiveData<ReceiverState>

    fun openRoute(pcapFile: String): Boolean
    fun closeRoute()
}