package org.ngbp.jsonrpc4jtestharness.rpc.manager

import java.lang.IllegalStateException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RPCManager @Inject constructor() {
    interface IReceiverCallback {
        fun updateViewPosition(scaleFactor: Double, xPos: Double, yPos: Double)
    }

    private var callback: IReceiverCallback? = null

    var language: String = Locale.getDefault().language
    var queryServiceId: String? = null

    fun setCallback(callback: IReceiverCallback) {
        if (this.callback != null) throw IllegalStateException("RPCManager callback already initialized")
        this.callback = callback
    }

    fun updateViewPosition(scaleFactor: Double?, xPos: Double?, yPos: Double?) {
        callback?.updateViewPosition(scaleFactor ?: 1.0, xPos ?: 0.0, yPos ?: 0.0)
    }
}