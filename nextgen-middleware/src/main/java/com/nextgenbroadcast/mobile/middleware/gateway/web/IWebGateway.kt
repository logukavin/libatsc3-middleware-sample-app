package com.nextgenbroadcast.mobile.middleware.gateway.web

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application

interface IWebGateway {
    val hostName: String
    var httpPort: Int
    var httpsPort: Int
    var wsPort: Int
    var wssPort: Int

    val selectedService: LiveData<SLSService?>
    val appCache: LiveData<List<Atsc3Application>>

    fun setPortByName(name: String, port: Int)

    companion object {
        const val TYPE_HTTP = "TYPE_HTTP"
        const val TYPE_HTTPS = "TYPE_HTTPS"
        const val TYPE_WS = "TYPE_WS"
        const val TYPE_WSS = "TYPE_WSS"
    }
}