package org.ngbp.jsonrpc4jtestharness.gateway.rpc

import androidx.lifecycle.Observer
import com.github.nmuzhichin.jsonrpc.model.request.Notification
import org.ngbp.jsonrpc4jtestharness.controller.service.IServiceController
import org.ngbp.jsonrpc4jtestharness.controller.view.IViewController
import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.core.ws.SocketHolder
import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType
import org.ngbp.jsonrpc4jtestharness.rpc.notification.model.ServiceChangeNotification
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCObjectMapperUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RPCGatewayImpl @Inject constructor(
        private val serviceController: IServiceController,
        private val viewController: IViewController,
        private val socketHolder: SocketHolder
) : IRPCGateway {
    private var subscribedINotifications = mutableSetOf<NotificationType>()

    override val language: String = java.util.Locale.getDefault().language
    override val queryServiceId: String?
        get() = serviceController.selectedService.value?.globalId
    override val mediaUrl: String?
        get() = viewController.rmpMediaUrl.value
    override val playbackState: PlaybackState
        get() = viewController.rmpState.value ?: PlaybackState.IDLE

    init {
        serviceController.selectedService.observeForever(Observer {

            if (subscribedINotifications.contains(NotificationType.SERVICE_CHANGE)) {
                val notification = ServiceChangeNotification(service = it?.globalId).create()
                sendNotification(notification)
            }
        })
    }

    override fun updateRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double) {
        viewController.updateRMPPosition(scaleFactor, xPos, yPos)
    }

    override fun requestMediaPlay(mediaUrl: String?, delay: Long) {
        viewController.requestMediaPlay(mediaUrl, delay)
    }

    override fun requestMediaStop(delay: Long) {
        viewController.requestMediaStop(delay)
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

    companion object {
        private val SUPPORTED_NOTIFICATIONS = setOf(
                NotificationType.SERVICE_CHANGE,
                NotificationType.SERVICE_GUIDE_CHANGE
        )
    }
}