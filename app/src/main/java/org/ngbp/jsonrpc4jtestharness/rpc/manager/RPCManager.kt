package org.ngbp.jsonrpc4jtestharness.rpc.manager

import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.ServiceGuideUrls
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RPCManager @Inject constructor() {
    val language = Locale.getDefault().language

    val queryServiceId = "tag:sinclairplatform.com,2020:WZTV:2727"

    private var callback: ReceiverActionCallback? = null

    fun getCallback(): ReceiverActionCallback? {
        return callback
    }

    fun setCallback(callback: ReceiverActionCallback?) {
        this.callback = callback
    }

    fun updateViewPosition(scaleFactor: Double?, xPos: Double?, yPos: Double?) {
        callback?.updateViewPosition(scaleFactor ?: 1.0, xPos ?: 0.0, yPos ?: 0.0)
    }
}