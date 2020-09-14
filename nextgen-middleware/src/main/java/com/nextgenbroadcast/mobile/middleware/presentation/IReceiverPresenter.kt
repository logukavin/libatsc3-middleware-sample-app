package com.nextgenbroadcast.mobile.middleware.presentation

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.mmt.atsc3.media.MMTDataSource

interface IReceiverPresenter {
    val receiverState: LiveData<ReceiverState>

    fun openRoute(path: String): Boolean
    fun closeRoute()

    fun createMMTSource(): MMTDataSource
}