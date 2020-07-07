package org.ngbp.jsonrpc4jtestharness.controller

import androidx.lifecycle.LiveData
import org.ngbp.libatsc3.Atsc3Module

interface IReceiverController {
    val state: LiveData<Atsc3Module.State>
    val sltServices: LiveData<List<SLSService>>

    val rpmParams: LiveData<RPMParams>

    fun openRoute(pcapFile: String): Boolean
    fun stopRoute()
    fun closeRoute()
}