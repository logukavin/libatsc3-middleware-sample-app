package org.ngbp.jsonrpc4jtestharness.presentation

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.core.model.SLSService

interface ISelectorPresenter {
    val sltServices: LiveData<List<SLSService>>

    fun selectService(service: SLSService)
}