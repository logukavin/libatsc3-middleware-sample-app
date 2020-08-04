package com.nextgenbroadcast.mobile.middleware.gateway.web

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.SLSService
import org.ngbp.libatsc3.entities.app.Atsc3Application

interface IWebGateway {
    val selectedService: LiveData<SLSService?>
    val appCache: LiveData<List<Atsc3Application>>
}