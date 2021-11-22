package com.nextgenbroadcast.mobile.core.ssdp

import kotlinx.coroutines.flow.MutableStateFlow

class SSDPTransportFactory(
    private val deviceId: String,
    private val logger: (String) -> Unit = {}
) {

    fun createTransport(
        address: String,
        port: Int,
        role: SSDPRole,
        ssdpDeviceInfoFlow: MutableStateFlow<Set<SSDPDeviceInfo>>
    ): ISSDPTransport = SSDPTransportImpl(
        address = address,
        port = port,
        role = role,
        deviceId = deviceId,
        ssdpDeviceInfoFlow = ssdpDeviceInfoFlow,
        logger = logger
    )

}