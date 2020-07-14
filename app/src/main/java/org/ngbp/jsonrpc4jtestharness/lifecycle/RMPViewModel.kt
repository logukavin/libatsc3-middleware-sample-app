package org.ngbp.jsonrpc4jtestharness.lifecycle

import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.ngbp.jsonrpc4jtestharness.controller.IReceiverController

class RMPViewModel(
        controller: IReceiverController
) : ViewModel() {
    val rmpParams = Transformations.distinctUntilChanged(controller.rpmParams)
}

