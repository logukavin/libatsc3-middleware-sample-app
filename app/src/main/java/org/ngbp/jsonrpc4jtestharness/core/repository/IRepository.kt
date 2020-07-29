package org.ngbp.jsonrpc4jtestharness.core.repository

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.core.model.AppData
import org.ngbp.jsonrpc4jtestharness.core.model.SLSService
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Urls
import org.ngbp.libatsc3.entities.app.Atsc3Application

interface IRepository {
    // Receiver
    val selectedService: LiveData<SLSService?>
    val serviceGuideUrls: LiveData<List<Urls>?>

    // Media Player
    val routeMediaUrl: LiveData<String?>

    // User Agent
    val applications: LiveData<List<Atsc3Application>>
    val services: LiveData<List<SLSService>>
    val appData: LiveData<AppData?>

    fun addOrUpdateApplication(application: Atsc3Application)
    fun setServices(services: List<SLSService>)
    fun setSelectedService(service: SLSService?)
    fun setAppEntryPoint(data: AppData?)
    fun setMediaUrl(mediaUrl: String?)
    fun reset()
}