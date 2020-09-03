package com.nextgenbroadcast.mobile.middleware.gateway.web

import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.settings.IServerSettings

internal class WebGatewayImpl (
        serviceController: IServiceController,
        repository: IRepository,
        settings: IServerSettings
) : IWebGateway {
    override val hostName = settings.hostName
    override var httpPort = settings.httpPort
    override val httpsPort = settings.httpsPort
    override val wsPort = settings.wsPort
    override val wssPort = settings.wssPort

    override val selectedService = serviceController.selectedService.distinctUntilChanged()
    override val appCache = repository.applications.map { it ?: emptyList() }
}