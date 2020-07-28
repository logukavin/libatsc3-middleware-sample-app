package org.ngbp.jsonrpc4jtestharness.rpc.notification

import com.github.nmuzhichin.jsonrpc.model.request.Notification
import org.ngbp.jsonrpc4jtestharness.gateway.rpc.IRPCGateway
import org.ngbp.jsonrpc4jtestharness.rpc.notification.model.RPCNotification
import org.ngbp.jsonrpc4jtestharness.rpc.notification.model.ServiceChangeNotification
import org.ngbp.jsonrpc4jtestharness.rpc.notification.model.ServiceGuideChangeNotification
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCObjectMapperUtils
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Urls

class RPCNotifier (private val gateway: IRPCGateway) {

    fun notifyServiceChange(serviceId: String) {
        val message = mapRPCNotificationToMessage(ServiceChangeNotification(service = serviceId))
        gateway.sendNotification(message)
    }

    fun notifyServiceGuideChange(urlList: List<Urls>) {
        val message = mapRPCNotificationToMessage(ServiceGuideChangeNotification(urlList = urlList))
        gateway.sendNotification(message)
    }

    private fun mapRPCNotificationToMessage(rpcNotification: RPCNotification): String {
        val notification = createNotificationWithParams(rpcNotification)
        return RPCObjectMapperUtils().objectToJson(notification)
    }

    private fun createNotificationWithParams(rpcNotification: RPCNotification): Notification {
        val params = RPCObjectMapperUtils().objectToMap(rpcNotification)
        return Notification(NOTIFICATION_METHOD_NAME, params as Map<String, Any>)
    }

    companion object {
        private const val NOTIFICATION_METHOD_NAME = "org.atsc.notify"
    }
}