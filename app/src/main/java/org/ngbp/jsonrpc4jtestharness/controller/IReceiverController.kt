package org.ngbp.jsonrpc4jtestharness.controller

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.controller.model.AppData
import org.ngbp.jsonrpc4jtestharness.controller.model.RPMParams
import org.ngbp.jsonrpc4jtestharness.controller.model.SLSService
import org.ngbp.libatsc3.Atsc3Module

interface IReceiverController {
    val state: LiveData<Atsc3Module.State>
    val sltServices: LiveData<List<SLSService>>

    val appData: LiveData<AppData?>

    val rpmParams: LiveData<RPMParams>

    fun openRoute(pcapFile: String): Boolean
    fun stopRoute()
    fun closeRoute()

    fun selectService(service: SLSService)

    fun resetRMP()
    fun rmpPlaybackChanged(state: Int)
}