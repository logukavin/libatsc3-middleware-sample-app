package com.nextgenbroadcast.mobile.middleware.repository

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.Urls
import org.ngbp.libatsc3.entities.app.Atsc3Application
import org.ngbp.libatsc3.entities.held.Atsc3HeldPackage

internal interface IRepository {
    // Receiver
    val selectedService: LiveData<SLSService?>
    val serviceGuideUrls: LiveData<List<Urls>?>

    // Media Player
    val routeMediaUrl: LiveData<String?>

    // User Agent
    val applications: LiveData<List<Atsc3Application>?>
    val services: LiveData<List<SLSService>>
    val heldPackage: LiveData<Atsc3HeldPackage?>

    fun addOrUpdateApplication(application: Atsc3Application)
    fun findApplication(appContextId: String): Atsc3Application?

    fun setServices(services: List<SLSService>)
    fun setSelectedService(service: SLSService?)
    fun setHeldPackage(data: Atsc3HeldPackage?)
    fun setMediaUrl(mediaUrl: String?)
    fun reset()
}