package com.nextgenbroadcast.mobile.middleware

import com.nextgenbroadcast.mobile.core.asReadOnly
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.middleware.analytics.IAtsc3Analytics
import com.nextgenbroadcast.mobile.middleware.atsc3.IAtsc3Module
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.SLTConstants
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.IServiceGuideDeliveryUnitReader
import com.nextgenbroadcast.mobile.middleware.atsc3.source.IAtsc3Source
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.service.ServiceControllerImpl
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.controller.view.ViewControllerImpl
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.service.provider.IMediaFileProvider
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import kotlinx.coroutines.*

internal class Atsc3ReceiverCore(
    private val atsc3Module: IAtsc3Module,
    val settings: IMiddlewareSettings,
    val repository: IRepository,
    private val serviceGuideReader: IServiceGuideDeliveryUnitReader,
    val mediaFileProvider: IMediaFileProvider,
    val analytics: IAtsc3Analytics
) : IAtsc3ReceiverCore {
    //TODO: create own scope?
    private val coreScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    val serviceController: IServiceController = ServiceControllerImpl(repository, settings, atsc3Module, analytics, serviceGuideReader, coreScope)
    val viewController: IViewController = ViewControllerImpl(analytics)

    var ignoreAudioServiceMedia: Boolean = true
        private set

    // event flows
    val errorFlow = serviceController.errorFlow.asReadOnly()
    val rfPhyMetricsFlow = atsc3Module.rfPhyMetricsFlow.asReadOnly()

    fun deInitialize() {
        mainScope.launch {
            serviceController.closeRoute()
        }

        // this instance wouldn't be destroyed so do not finish local scope
        // coreScope.cancel()
    }

    fun isIdle(): Boolean {
        return atsc3Module.isIdle()
    }

    override fun openRoute(source: IAtsc3Source, force: Boolean, onOpen: suspend (result: Boolean) -> Unit) {
        mainScope.launch {
            val result = serviceController.openRoute(source, force)
            onOpen(result)
        }
    }

    override fun closeRoute() {
        mainScope.launch {
            serviceController.closeRoute()
        }
    }

    override fun tune(frequency: PhyFrequency) {
        mainScope.launch {
            serviceController.tune(frequency)
        }
    }

    override fun selectService(service: AVService, block: suspend (result: Boolean) -> Unit) {
        mainScope.launch {
            val result = serviceController.selectService(service)
            block(result)
        }
    }

    override fun cancelScanning() {
        mainScope.launch {
            serviceController.cancelScanning()
        }
    }

    override fun getReceiverState(): ReceiverState {
        return serviceController.receiverState.value
    }

    fun getSelectedService(): AVService? {
        return repository.selectedService.value
    }

    fun getFrequency(): Int {
        return serviceController.receiverFrequency.value
    }

    fun getNextService() = serviceController.getNearbyService(1)

    fun getPreviousService() = serviceController.getNearbyService(-1)

    fun findServiceById(bsid: Int, serviceId: Int): AVService? {
        return repository.findServiceBy(bsid, serviceId)
    }

    fun findServiceById(globalId: String): AVService? {
        return repository.findServiceBy(globalId)
    }

    fun findServiceByName(name: String): AVService? {
        return repository.findServiceOrNull { it.shortName == name }
    }

    fun findActiveServiceById(serviceId: Int): AVService? {
        return findServiceById(atsc3Module.getSelectedBSID(), serviceId)
    }

    fun playEmbedded(service: AVService): Boolean {
        return ignoreAudioServiceMedia && service.category == SLTConstants.SERVICE_CATEGORY_AO
    }

    fun notifyNewSessionStarted() {
        repository.incSessionNum()
    }

    override fun getPhyVersionInfo(): Map<String, String?> {
        return atsc3Module.getVersionInfo()
    }

}