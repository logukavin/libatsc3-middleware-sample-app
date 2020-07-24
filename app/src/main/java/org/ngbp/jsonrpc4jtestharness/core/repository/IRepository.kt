package org.ngbp.jsonrpc4jtestharness.core.repository

import androidx.lifecycle.LiveData
import org.ngbp.jsonrpc4jtestharness.core.model.AppData
import org.ngbp.jsonrpc4jtestharness.core.model.SLSService

interface IRepository {
    // Receiver
    val selectedService : LiveData<SLSService?>

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