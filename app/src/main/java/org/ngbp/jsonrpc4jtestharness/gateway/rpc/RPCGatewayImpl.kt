package org.ngbp.jsonrpc4jtestharness.gateway.rpc

import org.ngbp.jsonrpc4jtestharness.controller.service.IServiceController
import org.ngbp.jsonrpc4jtestharness.controller.view.IViewController
import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RPCGatewayImpl @Inject constructor(
        private val serviceController: IServiceController,
        private val viewController: IViewController
) : IRPCGateway {
    private var subscribedINotifications = mutableSetOf<NotificationType>()

    override val language: String = java.util.Locale.getDefault().language
    override val queryServiceId: String?
        get() = serviceController.selectedService.value?.globalId
    override val mediaUrl: String?
        get() = viewController.rmpMediaUrl.value
    override val playbackState: PlaybackState
        get() = viewController.rmpState.value ?: PlaybackState.IDLE

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
        val available = SUPPORTEN_NOTIFICATIONS.toMutableSet()
        available.retainAll(requested)
        return available
    }

    companion object {
        private val SUPPORTEN_NOTIFICATIONS = setOf(
                NotificationType.SERVICE_CHANGE,
                NotificationType.SERVICE_GUIDE_CHANGE,
                NotificationType.ALERT_CHANGE,
                NotificationType.MPD_CHANGE
        )
    }
}