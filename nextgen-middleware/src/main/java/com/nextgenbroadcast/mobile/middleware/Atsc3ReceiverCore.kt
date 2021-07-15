package com.nextgenbroadcast.mobile.middleware

import android.net.Uri
import com.nextgenbroadcast.mobile.core.asReadOnly
import com.nextgenbroadcast.mobile.core.model.*
import com.nextgenbroadcast.mobile.middleware.analytics.IAtsc3Analytics
import com.nextgenbroadcast.mobile.middleware.atsc3.IAtsc3Module
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.SLTConstants
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.IServiceGuideDeliveryUnitReader
import com.nextgenbroadcast.mobile.middleware.atsc3.source.IAtsc3Source
import com.nextgenbroadcast.mobile.middleware.cache.IApplicationCache
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.service.ServiceControllerImpl
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.controller.view.ViewControllerImpl
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.RPCGatewayImpl
import com.nextgenbroadcast.mobile.middleware.gateway.web.IWebGateway
import com.nextgenbroadcast.mobile.middleware.gateway.web.WebGatewayImpl
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal class Atsc3ReceiverCore(
    private val atsc3Module: IAtsc3Module,
    val settings: IMiddlewareSettings,
    val repository: IRepository,
    private val serviceGuideReader: IServiceGuideDeliveryUnitReader,
    val analytics: IAtsc3Analytics
) : IAtsc3ReceiverCore {
    //TODO: create own scope?
    private val coreScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    private val serviceController: IServiceController = ServiceControllerImpl(repository, settings, atsc3Module, analytics, serviceGuideReader, coreScope)
    val viewController: IViewController = ViewControllerImpl(repository, analytics)

    var ignoreAudioServiceMedia: Boolean = true
        private set

    // event flows
    val errorFlow = serviceController.errorFlow.asReadOnly()
    val rfPhyMetricsFlow = atsc3Module.rfPhyMetricsFlow.asReadOnly()

    fun createWebGateway(): IWebGateway {
        return WebGatewayImpl(repository, settings)
    }

    fun createRPCGateway(appCache: IApplicationCache, scope: CoroutineScope): IRPCGateway {
        return RPCGatewayImpl(repository, viewController, serviceController, appCache, settings, scope)
    }

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

    override fun setRouteList(routes: List<RouteUrl>) {
        repository.setRoutes(routes)
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

    fun getSourceList(): List<RouteUrl> {
        return repository.routes.value
    }

    fun getSelectedService(): AVService? {
        return repository.selectedService.value
    }

    fun getServiceList(): List<AVService> {
        return serviceController.routeServices.value
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

    fun getMediaUri(mediaUrl: MediaUrl): Uri {
        return repository.getRouteMediaUri(mediaUrl)
    }

    fun notifyNewSessionStarted() {
        repository.incSessionNum()
    }

    override fun getPhyVersionInfo(): Map<String, String?> {
        return atsc3Module.getVersionInfo()
    }

    suspend inline fun observeReceiverState(crossinline action: suspend (state: ReceiverState) -> Unit) {
        serviceController.receiverState.collect(action)
    }

    suspend inline fun observeRouteServices(crossinline action: suspend (services: List<AVService>) -> Unit) {
        serviceController.routeServices.collect(action)
    }

    suspend inline fun observeCombinedState(playbackStateFlow: Flow<PlaybackState>, crossinline action: suspend (state: Triple<ReceiverState, AVService?, PlaybackState>) -> Unit) {
        combine(serviceController.receiverState, repository.selectedService, playbackStateFlow) { receiverState, selectedService, playbackState ->
            Triple(receiverState, selectedService, playbackState)
        }.stateIn(coreScope, SharingStarted.Eagerly, Triple(ReceiverState.idle(), null, PlaybackState.IDLE)).collect(action)
    }
}