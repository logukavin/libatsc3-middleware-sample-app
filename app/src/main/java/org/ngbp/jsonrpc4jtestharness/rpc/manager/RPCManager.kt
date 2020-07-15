package org.ngbp.jsonrpc4jtestharness.rpc.manager

import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.MPDUrl
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

    var queryServiceId: String? = "tag:sinclairplatform.com,2020:WZTV:2727" //TODO: remove after tests
    val MPDUrl: String = "http://127.0.0.1:8080/10.4/MPD.mpd" //TODO: remove after applying data source

    var playbackState: Int? = null
    fun setCallback(callback: IReceiverCallback) {
        if (this.callback != null) throw IllegalStateException("RPCManager callback already initialized")
        this.callback = callback
    }

    fun updateViewPosition(scaleFactor: Double?, xPos: Double?, yPos: Double?) {
        callback?.updateViewPosition(scaleFactor ?: 1.0, xPos ?: 0.0, yPos ?: 0.0)
    }
}