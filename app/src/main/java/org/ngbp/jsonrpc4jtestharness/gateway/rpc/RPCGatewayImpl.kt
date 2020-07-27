package org.ngbp.jsonrpc4jtestharness.gateway.rpc

import com.github.nmuzhichin.jsonrpc.model.request.Notification
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.ngbp.jsonrpc4jtestharness.controller.service.IServiceController
import org.ngbp.jsonrpc4jtestharness.controller.view.IViewController
import org.ngbp.jsonrpc4jtestharness.core.model.AppData
import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.core.repository.IRepository
import org.ngbp.jsonrpc4jtestharness.core.ws.SocketHolder
import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType
import org.ngbp.jsonrpc4jtestharness.rpc.notification.model.ServiceChangeNotification
import org.ngbp.jsonrpc4jtestharness.rpc.notification.model.ServiceGuideChangeNotification
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCObjectMapperUtils
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Urls
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RPCGatewayImpl @Inject constructor(
        private val serviceController: IServiceController,
        private val viewController: IViewController,
        private val repository: IRepository,
        private val socketHolder: SocketHolder
) : IRPCGateway {
    private val mainScope = MainScope()
    private val subscribedINotifications = mutableSetOf<NotificationType>()

    private var currentAppData: AppData? = null

    override val language: String = java.util.Locale.getDefault().language
    override val queryServiceId: String?
        get() = serviceController.selectedService.value?.globalId
    override val mediaUrl: String?
        get() = viewController.rmpMediaUrl.value
    override val playbackState: PlaybackState
        get() = viewController.rmpState.value ?: PlaybackState.IDLE
    override val serviceGuideUrls: List<Urls>
        get() = serviceController.serviceGuidUrls.value as List<Urls>

    init {
        repository.appData.observeForever{ appData ->
            onAppDataUpdated(appData)
        }

        serviceController.serviceGuidUrls.observeForever {
            if (subscribedINotifications.contains(NotificationType.SERVICE_GUIDE_CHANGE)) {
                val notification = ServiceGuideChangeNotification(urlList = it).create()
                sendNotification(notification)
            }
        }
    }

    override fun updateRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double) {
        mainScope.launch {
            viewController.updateRMPPosition(scaleFactor, xPos, yPos)
        }
    }

    override fun requestMediaPlay(mediaUrl: String?, delay: Long) {
        mainScope.launch {
            viewController.requestMediaPlay(mediaUrl, delay)
        }
    }

    override fun requestMediaStop(delay: Long) {
        mainScope.launch {
            viewController.requestMediaStop(delay)
        }
    }

    override fun subscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType> {
        val available = getAvailableNotifications(notifications)
        subscribedINotifications.addAll(available)
        return available
    }

    override fun unsubscribeNotifications(notifications: Set<NotificationType>): Set<NotificationType> {
        val available = getAvailableNotifications(notifications)
        subscribedINotifications.removeAll(available)
        return available
    }

    private fun getAvailableNotifications(requested: Set<NotificationType>): Set<NotificationType> {
        val available = SUPPORTED_NOTIFICATIONS.toMutableSet()
        available.retainAll(requested)
        return available
    }

    override fun sendNotification(notification: Notification) {
        socketHolder.broadcastMessage(RPCObjectMapperUtils.objectToJson(notification))
    }

    private fun onAppDataUpdated(appData: AppData?) {
        appData?.let {
            if (appData.isAppEquals(currentAppData)) {
                if (subscribedINotifications.contains(NotificationType.SERVICE_CHANGE)) {
                    val notification = ServiceChangeNotification(service = appData.appContextId).create()
                    sendNotification(notification)
                }
            }
        }
        currentAppData = appData
    }

    companion object {
        private val SUPPORTED_NOTIFICATIONS = setOf(
                NotificationType.SERVICE_CHANGE,
                NotificationType.SERVICE_GUIDE_CHANGE
        )
    }
}