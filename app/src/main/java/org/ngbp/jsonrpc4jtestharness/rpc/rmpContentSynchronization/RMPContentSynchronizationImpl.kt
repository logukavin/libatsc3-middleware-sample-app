package org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse
import org.ngbp.jsonrpc4jtestharness.rpc.manager.RPCManager
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model.RmpMediaTime
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model.RmpPlaybackRate
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model.RmpPlaybackState
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model.RmpWallClockTime

class RMPContentSynchronizationImpl(val rpcManager: RPCManager) : IRMPContentSynchronization {
    override fun queryRMPMediaTime(): RmpMediaTime {
        return RmpMediaTime()
    }

    override fun queryRMPWallClock(): RmpWallClockTime {
        return RmpWallClockTime()
    }

    private val NOT_PAYING_STATE = 2
    override fun queryRMPPlaybackState(): RmpPlaybackState {
        return RmpPlaybackState(rpcManager.playbackState ?: NOT_PAYING_STATE)
    }

    override fun queryRMPPlaybackRate(): RmpPlaybackRate {
        return RmpPlaybackRate()
    }

    override fun rMPMediaTimeChangeNotification(): RpcResponse {
        return RpcResponse()
    }
}