package org.ngbp.jsonrpc4jtestharness.controller.service

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.presentation.IReceiverPresenter
import org.ngbp.jsonrpc4jtestharness.presentation.ISelectorPresenter
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Urls

interface IServiceController : IReceiverPresenter, ISelectorPresenter {
    val serviceGuidUrls: LiveData<List<Urls>?>
}