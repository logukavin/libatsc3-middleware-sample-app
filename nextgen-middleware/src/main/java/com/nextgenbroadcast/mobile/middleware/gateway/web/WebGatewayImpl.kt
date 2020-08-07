package com.nextgenbroadcast.mobile.middleware.gateway.web

import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WebGatewayImpl @Inject constructor(
        serviceController: IServiceController,
        repository: IRepository
) : IWebGateway {
    override val hostName = repository.hostName
    override val httpPort = repository.httpPort
    override val httpsPort = repository.httpsPort
    override val wsPort = repository.wsPort
    override val wssPort = repository.wssPort

    override val selectedService = serviceController.selectedService.distinctUntilChanged()
    override val appCache = repository.applications.map { it ?: emptyList() }
}