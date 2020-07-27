package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

import com.github.nmuzhichin.jsonrpc.model.request.Notification
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCObjectMapperUtils

open class BaseNotification {

    fun create(): Notification {
        val properties = RPCObjectMapperUtils.objectToMap(this)
        return Notification(NOTIFICATION_METHOD_NAME, properties as Map<String, Any>)
    }

    companion object {
        private const val NOTIFICATION_METHOD_NAME = "org.atsc.notify"
    }
}