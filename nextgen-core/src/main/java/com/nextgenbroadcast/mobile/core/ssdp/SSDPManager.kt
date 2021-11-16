package com.nextgenbroadcast.mobile.core.ssdp

import kotlinx.coroutines.flow.*

class SSDPManager(
    val role: SSDPRole,
    private val ssdpTransportFactory: SSDPTransportFactory
) {

    private var ssdpTransport: ISSDPTransport? = null

    val isRunning: Boolean
        get() = ssdpTransport?.isRunning == true

    private val mDeviceFlow = MutableStateFlow<Set<SSDPDeviceInfo>>(mutableSetOf())
    val deviceFlow = mDeviceFlow.asStateFlow()

    fun start(location: String) {
        ssdpTransport = ssdpTransportFactory.createTransport(
            address = MULTICAST_ADDRESS,
            port = PORT,
            ssdpDeviceInfoFlow = mDeviceFlow,
            role = role
        ).apply {
            start()
            advertise(location)
        }
    }

    fun shutdown() {
        ssdpTransport?.shutdown()
    }

    fun search(searchTarget: String) {
        ssdpTransport?.search(searchTarget)
    }

    //TODO: need to return something ?
    fun search() {
        search(searchTarget = role.oppositeRole().data)
    }

    fun advertise(location: String) {
        ssdpTransport?.advertise(location)
    }

    companion object {
        internal const val PORT = 1900
        internal const val MULTICAST_ADDRESS = "239.255.255.250"
    }

}