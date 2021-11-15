package com.nextgenbroadcast.mobile.middleware.rpc.subscribeUnsubscribe

import com.nextgenbroadcast.mobile.middleware.rpc.RpcErrorCode
import com.nextgenbroadcast.mobile.middleware.rpc.RpcException
import com.nextgenbroadcast.mobile.middleware.rpc.notification.NotificationType
import com.nextgenbroadcast.mobile.middleware.rpc.subscribeUnsubscribe.model.LaunchParams
import com.nextgenbroadcast.mobile.middleware.rpc.subscribeUnsubscribe.model.SubscribeRpcResponse
import com.nextgenbroadcast.mobile.middleware.server.IApplicationSession

class SubscribeUnsubscribeImpl(
    private val session: IApplicationSession
) : ISubscribeUnsubscribe {

    override fun integratedSubscribe(msgType: List<String>, launchParams: List<LaunchParams>?): SubscribeRpcResponse {
        if (launchParams != null && launchParams.isNotEmpty()) {
            throw RpcException(RpcErrorCode.AUTOMATIC_LAUNCH_NOT_SUPPORTED)
        }

        val notifications = convertMsgTypeToNotifications(msgType)
        val subscribedNotifications = session.subscribeNotifications(notifications)

        return SubscribeRpcResponse(subscribedNotifications.map { it.value })
    }

    override fun integratedUnsubscribe(msgType: List<String>): SubscribeRpcResponse {
        val notifications = convertMsgTypeToNotifications(msgType)
        val unsubscribedNotifications = session.unsubscribeNotifications(notifications)

        return SubscribeRpcResponse(unsubscribedNotifications.map { it.value })
    }

    private fun convertMsgTypeToNotifications(msgType: List<String>): Set<NotificationType> {
        return when {
            msgType.isEmpty() -> emptySet()
            msgType.first() == "All" -> NotificationType.values().toSet()
            else -> NotificationType.values().filter{ msgType.contains(it.value) }.toSet()
        }
    }
}