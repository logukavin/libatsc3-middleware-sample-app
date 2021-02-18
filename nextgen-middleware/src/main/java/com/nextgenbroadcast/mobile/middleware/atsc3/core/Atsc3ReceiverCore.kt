package com.nextgenbroadcast.mobile.middleware.atsc3.core

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import com.nextgenbroadcast.mobile.core.cert.UserAgentSSLContext
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.presentation.*
import com.nextgenbroadcast.mobile.middleware.analytics.Atsc3Analytics
import com.nextgenbroadcast.mobile.middleware.analytics.IAtsc3Analytics
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3Module
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.IServiceGuideStore
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.ServiceGuideDeliveryUnitReader
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.db.RoomServiceGuideStore
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.db.SGDataBase
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
import com.nextgenbroadcast.mobile.middleware.repository.RepositoryImpl
import com.nextgenbroadcast.mobile.middleware.server.web.MiddlewareWebServer
import com.nextgenbroadcast.mobile.middleware.service.Atsc3ForegroundService
import com.nextgenbroadcast.mobile.middleware.service.init.FrequencyInitializer
import com.nextgenbroadcast.mobile.middleware.service.init.IServiceInitializer
import com.nextgenbroadcast.mobile.middleware.service.init.OnboardPhyInitializer
import com.nextgenbroadcast.mobile.middleware.service.init.UsbPhyInitializer
import com.nextgenbroadcast.mobile.middleware.service.provider.IMediaFileProvider
import com.nextgenbroadcast.mobile.middleware.service.provider.MediaFileProvider
import com.nextgenbroadcast.mobile.middleware.service.provider.esgProvider.ESGContentAuthority
import com.nextgenbroadcast.mobile.middleware.settings.IMiddlewareSettings
import com.nextgenbroadcast.mobile.middleware.settings.MiddlewareSettingsImpl
import kotlinx.coroutines.Dispatchers
import java.lang.ref.WeakReference

internal class Atsc3ReceiverCore private constructor(
        private val context: Context,
        private val atsc3Module: Atsc3Module,
        private val settings: IMiddlewareSettings,
        private val repository: IRepository,
        private val serviceGuideStore: IServiceGuideStore,
        private val analytics: IAtsc3Analytics
) : IAtsc3ServiceCore {
    //TODO: we should close this instances
    private val serviceGuideReader = ServiceGuideDeliveryUnitReader(serviceGuideStore)
    val serviceController: IServiceController = ServiceControllerImpl(repository, settings, atsc3Module, analytics, serviceGuideReader, Dispatchers.IO, ::onError)
    var viewController: IViewController? = null
        private set
    var ignoreAudioServiceMedia: Boolean = true
        private set

    private var webGateway: IWebGateway? = null
    private var rpcGateway: IRPCGateway? = null
    private var webServer: MiddlewareWebServer? = null
    val mediaFileProvider: IMediaFileProvider by lazy {
        MediaFileProvider(context)
    }

    private val initializer = ArrayList<WeakReference<IServiceInitializer>>()

    init {
        serviceGuideStore.subscribe {
            context.contentResolver.notifyChange(ESGContentAuthority.getServiceContentUri(context), null)
        }
    }

    fun deInitialize() {
        initializer.forEach { ref ->
            ref.get()?.cancel()
        }

        stopAndDestroyViewPresentation()
        atsc3Module.close()
    }

    fun initialize(components: Map<Class<*>, Pair<Int, String>>) {
        try {
            FrequencyInitializer(settings, this).also {
                initializer.add(WeakReference(it))
            }.initialize(context, components)

            val phyInitializer = OnboardPhyInitializer(this).also {
                initializer.add(WeakReference(it))
            }

            if (!phyInitializer.initialize(context, components)) {
                UsbPhyInitializer().also {
                    initializer.add(WeakReference(it))
                }.initialize(context, components)
            }
        } catch (e: Exception) {
            Log.d(Atsc3ForegroundService.TAG, "Can't initialize, something is wrong in metadata", e)
        }
    }

    fun createAndStartViewPresentation(downloadManager: IDownloadManager, lifecycleOwner: LifecycleOwner, ignoreAudioServiceMedia: Boolean): IViewController {
        this.ignoreAudioServiceMedia = ignoreAudioServiceMedia

        val appCache = ApplicationCache(atsc3Module.jni_getCacheDir(), downloadManager)

        val view = ViewControllerImpl(repository, settings, mediaFileProvider, analytics, ignoreAudioServiceMedia).apply {
            start(lifecycleOwner)
        }.also {
            viewController = it
        }
        val web = WebGatewayImpl(serviceController, repository, settings).also {
            webGateway = it
        }
        val rpc = RPCGatewayImpl(view, serviceController, appCache, settings, Dispatchers.Main, Dispatchers.IO).apply {
            start(lifecycleOwner)
        }.also {
            rpcGateway = it
        }

        view.appState.observe(lifecycleOwner) { appState ->
            when (appState) {
                ApplicationState.OPENED -> analytics.startApplicationSession()
                ApplicationState.LOADED,
                ApplicationState.UNAVAILABLE -> analytics.finishApplicationSession()
                else -> {
                    // ignore
                }
            }
        }

        startWebServer(rpc, web)

        return view
    }

    fun stopAndDestroyViewPresentation() {
        stopWebServer()

        webGateway = null
        rpcGateway = null
        viewController = null
    }

    private fun startWebServer(rpc: IRPCGateway, web: IWebGateway) {
        webServer = MiddlewareWebServer.Builder()
                .rpcGateway(rpc)
                .webGateway(web)
                .build().also {
                    it.start(UserAgentSSLContext(context))
                }
    }

    private fun stopWebServer() {
        webServer?.let { server ->
            if (server.isRunning()) {
                try {
                    server.stop()
                } catch (e: Exception) {
                    e.printStackTrace()
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

    override fun getReceiverState(): ReceiverState {
        return serviceController.receiverState.value ?: ReceiverState.IDLE
    }

    private fun onError(message: String) {
        //super hack activity.runOnUiThread() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun getNextService() = getNearbyService(1)

    fun getPreviousService() = getNearbyService(-1)

    private fun getNearbyService(offset: Int): AVService? {
        return repository.selectedService.value?.let { activeService ->
            repository.services.value?.let { services ->
                val activeServiceIndex = services.indexOf(activeService)
                services.getOrNull(activeServiceIndex + offset)
            }
        }
    }

    fun findServiceBy(bsid: Int, serviceId: Int): AVService? {
        return repository.findServiceBy(bsid, serviceId)
    }

    fun findActiveServiceById(serviceId: Int): AVService? {
        return findServiceBy(atsc3Module.selectedServiceBsid, serviceId)
    }

    fun getCurrentlyPlayingMediaUri(): Uri? {
        getCurrentlyPlayingMediaUrl()?.let { mediaPath ->
            return mediaFileProvider.getMediaFileUri(mediaPath.url)
        }

        return null
    }

    fun getCurrentlyPlayingMediaUrl() = repository.routeMediaUrl.value

    companion object {
        @Volatile
        private var INSTANCE: Atsc3ReceiverCore? = null

        @JvmStatic
        fun getInstance(context: Context): Atsc3ReceiverCore {
            val appContext = context.applicationContext

            val instance = INSTANCE
            return instance ?: synchronized(this) {
                val instance2 = INSTANCE
                instance2 ?: let {
                    val settings = MiddlewareSettingsImpl.getInstance(appContext)
                    val repository = RepositoryImpl()
                    val serviceGuideStore = RoomServiceGuideStore(SGDataBase.getDatabase(appContext))
                    val atsc3Module = Atsc3Module(appContext.cacheDir)
                    val analytics = Atsc3Analytics.getInstance(appContext, settings)

                    Atsc3ReceiverCore(appContext, atsc3Module, settings, repository, serviceGuideStore, analytics).also {
                        INSTANCE = it
                    }
                }
            }
        }
    }
}