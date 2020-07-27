package org.ngbp.jsonrpc4jtestharness.core.repository

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.core.model.AppData
import org.ngbp.jsonrpc4jtestharness.core.model.SLSService
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Urls

interface IRepository {
    // Receiver
    val selectedService : LiveData<SLSService?>
    val serviceGuideUrls : LiveData<List<Urls>?>

    // Media Player
    val routeMediaUrl : LiveData<String?>

    // User Agent
    val availableServices : LiveData<List<SLSService>>
    val appData : LiveData<AppData?>

    fun reset()
    fun setSelectedService(service: SLSService?)
    fun setServices(services: List<SLSService>)
    fun setAppEntryPoint(data: AppData?)
    fun setMediaUrl(mediaUrl: String?)
}