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
    override var httpsPort = settings.httpsPort
    override var wsPort = settings.wsPort
    override var wssPort = settings.wssPort

    override val selectedService = serviceController.selectedService.distinctUntilChanged()
    override val appCache = repository.applications.map { it ?: emptyList() }

    override fun setPortByName(name: String, port: Int) {
        when (name) {
            IWebGateway.TYPE_HTTP -> httpPort = port
            IWebGateway.TYPE_WS -> wsPort = port
            IWebGateway.TYPE_HTTPS -> httpPort = port
            IWebGateway.TYPE_WSS -> wssPort = port
        }
    }
}