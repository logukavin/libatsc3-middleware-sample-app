package com.nextgenbroadcast.mobile.core.ssdp

import kotlinx.coroutines.flow.*

class SSDPManager(
    override val role: SSDPRole,
    private val ssdpTransportFactory: SSDPTransportFactory
) : ISSDPManager {

    private var ssdpTransport: ISSDPTransport? = null

    override val isRunning: Boolean
        get() = ssdpTransport?.isRunning == true

    private val _deviceFlow = MutableStateFlow<Set<SSDPDeviceInfo>>(mutableSetOf())
    override val deviceFlow = _deviceFlow.asStateFlow()

    override fun start(location: String) {
        ssdpTransport = ssdpTransportFactory.createTransport(
            address = MULTICAST_ADDRESS,
            port = PORT,
            ssdpDeviceInfoFlow = _deviceFlow,
            role = role
        ).apply {
            start()
            advertise(location)
        }
    }

    override fun shutdown() {
        _deviceFlow.value = emptySet()
        ssdpTransport?.shutdown()
    }

    fun search(searchTarget: String) {
        ssdpTransport?.search(searchTarget)
    }

    override fun search() {
        search(searchTarget = role.oppositeRole().data)
    }

    override fun advertise(location: String) {
        ssdpTransport?.advertise(location)
    }

    companion object {
        internal const val PORT = 1900
        internal const val MULTICAST_ADDRESS = "239.255.255.250"
    }

}