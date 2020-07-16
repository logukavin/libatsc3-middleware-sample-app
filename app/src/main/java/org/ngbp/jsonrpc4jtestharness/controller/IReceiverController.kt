package org.ngbp.jsonrpc4jtestharness.controller

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.controller.model.AppData
import org.ngbp.jsonrpc4jtestharness.controller.model.RPMParams
import org.ngbp.jsonrpc4jtestharness.controller.model.SLSService
import org.ngbp.libatsc3.Atsc3Module

interface IReceiverController {
    val state: LiveData<Atsc3Module.State>

    fun openRoute(pcapFile: String): Boolean
    fun stopRoute()
    fun closeRoute()
}