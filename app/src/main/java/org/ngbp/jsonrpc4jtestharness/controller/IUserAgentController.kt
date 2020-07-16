package org.ngbp.jsonrpc4jtestharness.controller

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.controller.model.AppData
import org.ngbp.jsonrpc4jtestharness.controller.model.SLSService

interface IUserAgentController {
    val sltServices: LiveData<List<SLSService>>
    val appData: LiveData<AppData?>

    fun selectService(service: SLSService)
}