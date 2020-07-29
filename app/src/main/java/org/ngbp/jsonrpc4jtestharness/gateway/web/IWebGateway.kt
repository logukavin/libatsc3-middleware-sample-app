package org.ngbp.jsonrpc4jtestharness.gateway.web

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.core.model.SLSService
import org.ngbp.libatsc3.entities.app.Atsc3Application

interface IWebGateway {
    val selectedService: LiveData<SLSService?>
    val appCache: LiveData<List<Atsc3Application>>
}