package org.ngbp.jsonrpc4jtestharness.rpc.processor

class RPCManager {
    val numberOfTuners = 1
    val yearOfMfr = 2017

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

    companion object {
        private val instance: RPCManager? = null
    }


}