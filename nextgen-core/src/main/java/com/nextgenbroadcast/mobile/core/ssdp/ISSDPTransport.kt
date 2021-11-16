package com.nextgenbroadcast.mobile.core.ssdp

interface ISSDPTransport {

    val isRunning: Boolean

    fun start()

    fun advertise(location: String)

    fun search(searchTarget: String)

    fun shutdown()

}