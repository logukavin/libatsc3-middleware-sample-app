package org.ngbp.jsonrpc4jtestharness.presentation

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.core.model.ReceiverState

interface IReceiverPresenter {
    val receiverState: LiveData<ReceiverState>

    fun openRoute(pcapFile: String): Boolean
    fun stopRoute()
    fun closeRoute()
}