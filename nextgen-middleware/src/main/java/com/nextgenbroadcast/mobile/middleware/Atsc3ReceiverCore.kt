package com.nextgenbroadcast.mobile.middleware

import com.nextgenbroadcast.mobile.middleware.server.cert.IUserAgentSSLContext
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.presentation.*
import com.nextgenbroadcast.mobile.middleware.analytics.IAtsc3Analytics
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3Module
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3ModuleState
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
import com.nextgenbroadcast.mobile.middleware.server.web.MiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.service.provider.IMediaFileProvider
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect

internal class Atsc3ReceiverCore(
        private val atsc3Module: Atsc3Module,
        val settings: IMiddlewareSettings,
        val repository: IRepository,
        private val serviceGuideStore: IServiceGuideStore,
        val mediaFileProvider: IMediaFileProvider,
        private val sslContext: IUserAgentSSLContext,
        private val analytics: IAtsc3Analytics
) : IAtsc3ServiceCore {
    //TODO: create own scope?
    private val coreScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private var viewScope: CoroutineScope? = null

    private val _errorFlow = MutableSharedFlow<String>(10, 0, BufferOverflow.DROP_OLDEST)
    val errorFlow = _errorFlow.asSharedFlow()

    //TODO: we should close this instances
    private val serviceGuideReader = ServiceGuideDeliveryUnitReader(serviceGuideStore)
    val serviceController: IServiceController = ServiceControllerImpl(repository, settings, atsc3Module, analytics, serviceGuideReader, coreScope, ::onError)
    var viewController: IViewController? = null
        private set
    var ignoreAudioServiceMedia: Boolean = true
        private set

    private var webGateway: IWebGateway? = null
    private var rpcGateway: IRPCGateway? = null
    private var webServer: MiddlewareWebServer? = null

    // event flows
    val rfPhyMetricsFlow = atsc3Module.rfPhyMetricsFlow.asSharedFlow()

    fun deInitialize() {
        stopAndDestroyViewPresentation()
        atsc3Module.close()

        // this instance wouldn't be destroyed so do not finish local scope
        // coreScope.cancel()
    }

    fun isIdle(): Boolean {
        return atsc3Module.getState() == Atsc3ModuleState.IDLE
    }

    fun createAndStartViewPresentation(downloadManager: IDownloadManager, ignoreAudioServiceMedia: Boolean,
                                       onObserve: (view: IViewController, viewScope: CoroutineScope) -> Unit): IViewController {
        this.ignoreAudioServiceMedia = ignoreAudioServiceMedia

        internalDestroyViewPresentation()

        val stateScope = CoroutineScope(Dispatchers.Default).also {
            viewScope = it
        }

        val appCache = ApplicationCache(atsc3Module.jni_getCacheDir(), downloadManager)

        val view = ViewControllerImpl(repository, settings, mediaFileProvider, analytics, stateScope).also {
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

        startWebServer(rpc, web, stateScope)

        return view
    }

    fun suspendViewPresentation() {
        stopWebServer()
    }

    fun resumeViewPresentation() {
        if (webServer != null) return

        val rpc = rpcGateway ?: return
        val web = webGateway ?: return
        val stateScope = viewScope ?: return

        stateScope.launch {
            viewController?.appData?.collect()
        }

        startWebServer(rpc, web, stateScope)
    }

    private fun stopAndDestroyViewPresentation() {
        suspendViewPresentation()
        internalDestroyViewPresentation()
    }

    private fun internalDestroyViewPresentation() {
        webGateway = null
        rpcGateway = null
        viewController = null

        viewScope?.cancel()
        viewScope = null
    }

    private fun startWebServer(rpc: IRPCGateway, web: IWebGateway, stateScope: CoroutineScope) {
        webServer = MiddlewareWebServer.Builder()
                .stateScope(stateScope)
                .rpcGateway(rpc)
                .webGateway(web)
                .build().also { server ->
                    GlobalScope.launch {
                        server.start(sslContext)
                        viewController?.onNewSessionStarted() // used to rebuild data related to server
                    }
                }
    }

    private fun stopWebServer() {
        webServer?.let { server ->
            if (server.isRunning()) {
                GlobalScope.launch {
                    try {
                        server.stop()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        webServer = null
    }

    override fun openRoute(filePath: String): Boolean {
        return serviceController.openRoute(filePath)
    }

    override fun openRoute(source: IAtsc3Source): Boolean {
        return serviceController.openRoute(source)
    }

    override fun closeRoute() {
        serviceController.stopRoute() // call to stopRoute is not a mistake. We use it to close previously opened file
        serviceController.closeRoute()
    }

    override fun tune(frequency: PhyFrequency) {
        serviceController.tune(frequency)
    }

    override fun selectService(service: AVService) {
        serviceController.selectService(service)
    }

    override fun getReceiverState(): ReceiverState {
        return serviceController.receiverState.value
    }

    private fun onError(message: String) {
        _errorFlow.tryEmit(message)
    }

    fun getNextService() = serviceController.getNearbyService(1)

    fun getPreviousService() = serviceController.getNearbyService(-1)

    fun findServiceBy(bsid: Int, serviceId: Int): AVService? {
        return repository.findServiceBy(bsid, serviceId)
    }

    fun findServiceBy(name: String): AVService? {
        return repository.findServiceOrNull { it.shortName == name }
    }

    fun findActiveServiceById(serviceId: Int): AVService? {
        return findServiceBy(atsc3Module.selectedServiceBsid, serviceId)
    }

    fun playEmbedded(service: AVService): Boolean {
        return ignoreAudioServiceMedia && service.category == SLTConstants.SERVICE_CATEGORY_AO
    }
}