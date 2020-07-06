package org.ngbp.jsonrpc4jtestharness.rpc.processor

import android.annotation.SuppressLint
import android.os.Build
import java.text.SimpleDateFormat

class RPCManager {
    val numberOfTuners = 1
    @SuppressLint("SimpleDateFormat")
    val yearOfMfr = SimpleDateFormat("yyyy").format(Build.TIME).toInt()

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