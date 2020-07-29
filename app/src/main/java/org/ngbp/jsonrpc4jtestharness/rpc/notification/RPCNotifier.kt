package org.ngbp.jsonrpc4jtestharness.rpc.notification

import com.github.nmuzhichin.jsonrpc.model.request.Notification
import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.gateway.rpc.IRPCGateway
import org.ngbp.jsonrpc4jtestharness.rpc.notification.model.*
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCObjectMapper
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Urls

class RPCNotifier (private val gateway: IRPCGateway) {

    fun notifyServiceChange(serviceId: String) {
        sendNotification(ServiceChangeNotification(serviceId))
    }

    fun notifyServiceGuideChange(urlList: List<Urls>) {
        sendNotification(ServiceGuideChangeNotification(urlList))
    }

    fun notifyMPDChange() {
        sendNotification(MPDChangeNotification())
    }

    fun notifyRmpPlaybackStateChange(playbackState: PlaybackState) {
        sendNotification(RmpPlaybackStateChangeNotification(playbackState))
    }

    fun notifyRmpMediaTimeChange(currentTime: Double) {
        sendNotification(RmpMediaTimeChangeNotification(currentTime))
    }

    fun notifyRmpPlaybackRateChange(playbackRate: Float) {
        sendNotification(RmpPlaybackRateChangeNotification(playbackRate))
    }

    private fun sendNotification(rpcNotification: RPCNotification) {
        val rpcObjectMapper = RPCObjectMapper()
        val params: Map<String, Any> = rpcObjectMapper.objectToMap(rpcNotification)
        val notification = Notification(NOTIFICATION_METHOD_NAME, params)
        val message = rpcObjectMapper.objectToJson(notification)

//        Just test solution
        val notificationThread: Thread = object : Thread() {
            override fun run() {
                gateway.sendNotification(message)
            }
        }
        notificationThread.start()
    }

    companion object {
        private const val NOTIFICATION_METHOD_NAME = "org.atsc.notify"
    }
}