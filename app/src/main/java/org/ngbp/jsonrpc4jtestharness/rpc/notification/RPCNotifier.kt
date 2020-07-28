package org.ngbp.jsonrpc4jtestharness.rpc.notification

import com.github.nmuzhichin.jsonrpc.model.request.Notification
import org.ngbp.jsonrpc4jtestharness.gateway.rpc.IRPCGateway
import org.ngbp.jsonrpc4jtestharness.rpc.notification.model.RPCNotification
import org.ngbp.jsonrpc4jtestharness.rpc.notification.model.ServiceChangeNotification
import org.ngbp.jsonrpc4jtestharness.rpc.notification.model.ServiceGuideChangeNotification
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCObjectMapper
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Urls

class RPCNotifier (private val gateway: IRPCGateway) {

    fun notifyServiceChange(serviceId: String) {
        sendNotification(ServiceChangeNotification(service = serviceId))
    }

    fun notifyServiceGuideChange(urlList: List<Urls>) {
        sendNotification(ServiceGuideChangeNotification(urlList = urlList))
    }

    private fun sendNotification(rpcNotification: RPCNotification) {
        val rpcObjectMapper = RPCObjectMapper()
        val params: Map<String, Any> = rpcObjectMapper.objectToMap(rpcNotification)
        val notification = Notification(NOTIFICATION_METHOD_NAME, params)
        val message = rpcObjectMapper.objectToJson(notification)
        gateway.sendNotification(message)
    }

    companion object {
        private const val NOTIFICATION_METHOD_NAME = "org.atsc.notify"
    }
}