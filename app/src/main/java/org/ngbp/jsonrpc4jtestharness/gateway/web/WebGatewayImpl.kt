package org.ngbp.jsonrpc4jtestharness.gateway.web

import androidx.lifecycle.distinctUntilChanged
import org.ngbp.jsonrpc4jtestharness.controller.service.IServiceController
import org.ngbp.jsonrpc4jtestharness.core.repository.IRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebGatewayImpl @Inject constructor(
        private val serviceController: IServiceController,
        repository: IRepository
) : IWebGateway {
    override val selectedService = serviceController.selectedService.distinctUntilChanged()
    override val appCache = repository.applications
}