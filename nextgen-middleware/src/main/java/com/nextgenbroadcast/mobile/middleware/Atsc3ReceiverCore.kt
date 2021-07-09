package com.nextgenbroadcast.mobile.middleware

import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.presentation.*
import com.nextgenbroadcast.mobile.middleware.analytics.IAtsc3Analytics
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3Module
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.SLTConstants
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.IServiceGuideStore
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.ServiceGuideDeliveryUnitReader
import com.nextgenbroadcast.mobile.middleware.atsc3.source.IAtsc3Source
import com.nextgenbroadcast.mobile.middleware.cache.ApplicationCache
import com.nextgenbroadcast.mobile.middleware.cache.IDownloadManager
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.service.ServiceControllerImpl
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.controller.view.ViewControllerImpl
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.RPCGatewayImpl
import com.nextgenbroadcast.mobile.middleware.gateway.web.IWebGateway
import com.nextgenbroadcast.mobile.middleware.gateway.web.WebGatewayImpl
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.service.provider.IMediaFileProvider
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

internal class Atsc3ReceiverCore(
        private val atsc3Module: Atsc3Module,
        val settings: IMiddlewareSettings,
        val repository: IRepository,
        private val serviceGuideStore: IServiceGuideStore,
        val mediaFileProvider: IMediaFileProvider,
        val analytics: IAtsc3Analytics
) : IAtsc3ReceiverCore {
    //TODO: create own scope?
    private val coreScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    private var viewScope: CoroutineScope? = null

    private val _errorFlow = MutableSharedFlow<String>(0, 10, BufferOverflow.DROP_OLDEST)
    val errorFlow = _errorFlow.asSharedFlow()

    private val serviceGuideReader = ServiceGuideDeliveryUnitReader(serviceGuideStore)
    val serviceController: IServiceController = ServiceControllerImpl(repository, settings, atsc3Module, analytics, serviceGuideReader, coreScope, ::onError)
    var viewController: IViewController? = null
        private set
    var ignoreAudioServiceMedia: Boolean = true
        private set

    private var webGateway: IWebGateway? = null
    private var rpcGateway: IRPCGateway? = null

    // event flows
    val rfPhyMetricsFlow = atsc3Module.rfPhyMetricsFlow.asSharedFlow()

    fun deInitialize() {
        destroyViewPresentation()
        mainScope.launch {
            serviceController.closeRoute()
        }

        // this instance wouldn't be destroyed so do not finish local scope
        // coreScope.cancel()
    }

    fun isIdle(): Boolean {
        return atsc3Module.isIdle()
    }

    fun createViewPresentation(downloadManager: IDownloadManager, ignoreAudioServiceMedia: Boolean,
                               onObserve: (view: IViewController, viewScope: CoroutineScope) -> Unit): IViewController {
        this.ignoreAudioServiceMedia = ignoreAudioServiceMedia

        destroyViewPresentation()

        val stateScope = CoroutineScope(Dispatchers.Default).also {
            viewScope = it
        }

        val appCache = ApplicationCache(atsc3Module.jni_getCacheDir(), downloadManager)

        val view = ViewControllerImpl(repository, mediaFileProvider, analytics, stateScope).also {
            viewController = it
        }
        val web = WebGatewayImpl(serviceController, settings).also {
            webGateway = it
        }
        val rpc = RPCGatewayImpl(view, serviceController, appCache, settings, stateScope).also {
            rpcGateway = it
        }

        //TODO: Analytics should listen to ViewController and not vice versa
        stateScope.launch {
            view.appState.collect { appState ->
                when (appState) {
                    ApplicationState.OPENED -> analytics.startApplicationSession()
                    ApplicationState.LOADED,
                    ApplicationState.UNAVAILABLE -> analytics.finishApplicationSession()
                    else -> {
                        // ignore
                    }
                }
            }
        }

        onObserve(view, stateScope)

        return view
    }

    private fun destroyViewPresentation() {
        webGateway = null
        rpcGateway = null
        viewController = null

        viewScope?.cancel()
        viewScope = null
    }

    fun getWebInterface(): Triple<IWebGateway, IRPCGateway, CoroutineScope>? {
        val web = webGateway ?: return null
        val rpc = rpcGateway ?: return null
        val stateScope = viewScope ?: return null

        return Triple(web, rpc, stateScope)
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

    private fun onError(message: String) {
        _errorFlow.tryEmit(message)
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
        return findServiceById(atsc3Module.selectedServiceBsid, serviceId)
    }

    fun playEmbedded(service: AVService): Boolean {
        return ignoreAudioServiceMedia && service.category == SLTConstants.SERVICE_CATEGORY_AO
    }

    fun notifyNewSessionStarted() {
        repository.onNewSessionStarted()
    }

    override fun getPhyVersionInfo(): Map<String, String?> {
        return atsc3Module.getVersionInfo()
    }

}