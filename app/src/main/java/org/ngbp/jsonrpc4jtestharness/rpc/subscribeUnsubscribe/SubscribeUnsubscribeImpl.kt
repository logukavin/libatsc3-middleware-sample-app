package org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe

import org.ngbp.jsonrpc4jtestharness.controller.IRPCController
import org.ngbp.jsonrpc4jtestharness.rpc.notification.NotificationType
import org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe.model.Subscribe

class SubscribeUnsubscribeImpl(private val rpcController: IRPCController) : ISubscribeUnsubscribe {
    override fun integratedSubscribe(msgType: List<String>): Subscribe {
        val notifications = convertMsgTypeToNotifications(msgType)
        val subscribedNotifications = rpcController.subscribeNotifications(notifications)

        return Subscribe(subscribedNotifications.map { it.value })
    }

    override fun integratedUnsubscribe(msgType: List<String>): Subscribe {
        val notifications = convertMsgTypeToNotifications(msgType)
        val unsubscribedNotifications = rpcController.subscribeNotifications(notifications)

        return Subscribe(unsubscribedNotifications.map { it.value })
    }

    private fun convertMsgTypeToNotifications(msgType: List<String>): Set<NotificationType> {
        return when {
            msgType.isEmpty() -> emptySet()
            msgType.first() == "All" -> NotificationType.values().toSet()
            else -> NotificationType.values().filter{ msgType.contains(it.value) }.toSet()
        }
    }
}