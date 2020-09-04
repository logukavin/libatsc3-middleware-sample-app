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

    override fun setPortByType(connectionType: ConnectionType, port: Int) {
        when (connectionType) {
            ConnectionType.HTTP -> httpPort = port
            ConnectionType.WS -> wsPort = port
            ConnectionType.HTTPS -> httpPort = port
            ConnectionType.WSS -> wssPort = port
        }
    }
}