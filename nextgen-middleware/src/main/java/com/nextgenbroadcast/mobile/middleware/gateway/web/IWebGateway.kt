package com.nextgenbroadcast.mobile.middleware.gateway.web

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application

interface IWebGateway {
    val hostName: String
    val httpPort: Int
    val httpsPort: Int
    val wsPort: Int
    val wssPort: Int

    val selectedService: LiveData<SLSService?>
    val appCache: LiveData<List<Atsc3Application>>
}