package org.ngbp.jsonrpc4jtestharness.controller.service

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.core.model.SLSService
import org.ngbp.jsonrpc4jtestharness.presentation.IReceiverPresenter
import org.ngbp.jsonrpc4jtestharness.presentation.ISelectorPresenter

interface IServiceController : IReceiverPresenter, ISelectorPresenter {
    val selectedService: LiveData<SLSService?>
}