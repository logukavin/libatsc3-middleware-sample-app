package com.nextgenbroadcast.mobile.core.ssdp

import kotlinx.coroutines.flow.StateFlow

interface ISSDPManager {

    val role: SSDPRole

    val isRunning: Boolean

    val deviceFlow: StateFlow<Set<SSDPDeviceInfo>>

    fun start(location: String)

    fun shutdown()

    fun search()

    fun advertise(location: String)

}