package org.ngbp.jsonrpc4jtestharness.gateway.web

import androidx.lifecycle.Transformations
import org.ngbp.jsonrpc4jtestharness.controller.service.IServiceController
import org.ngbp.jsonrpc4jtestharness.core.repository.IRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebGatewayImpl @Inject constructor(
        private val serviceController: IServiceController,
        repository: IRepository
) : IWebGateway {
    override val selectedService = Transformations.distinctUntilChanged(serviceController.selectedService)
    override val appCache = repository.applications
}