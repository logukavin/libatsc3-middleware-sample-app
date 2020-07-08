package org.ngbp.jsonrpc4jtestharness.lifecycle

import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.ngbp.jsonrpc4jtestharness.controller.IReceiverController

class RMPViewModel(
        controller: IReceiverController
) : ViewModel() {

    val rmpScale = Transformations.map(controller.rpmParams) { it.scale }
    val rmpX = Transformations.map(controller.rpmParams) { it.x }
    val rmpY = Transformations.map(controller.rpmParams) { it.y }

}