package org.ngbp.jsonrpc4jtestharness.controller

import androidx.lifecycle.LiveData
import org.ngbp.libatsc3.Atsc3Module

interface IReceiverController {
    val receiverState: LiveData<Atsc3Module.State>

    fun openRoute(pcapFile: String): Boolean
    fun stopRoute()
    fun closeRoute()
}