package org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization

import org.ngbp.jsonrpc4jtestharness.controller.IRPCController
import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model.RmpMediaTime
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model.RmpPlaybackRate
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model.RmpPlaybackState
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model.RmpWallClockTime

class RMPContentSynchronizationImpl(
        private val rpcController: IRPCController
) : IRMPContentSynchronization {
    override fun queryRMPMediaTime(): RmpMediaTime {
        return RmpMediaTime()
    }

    override fun queryRMPWallClock(): RmpWallClockTime {
        return RmpWallClockTime()
    }

    override fun queryRMPPlaybackState(): RmpPlaybackState {
        return RmpPlaybackState(rpcController.playbackState.state)
    }

    override fun queryRMPPlaybackRate(): RmpPlaybackRate {
        return RmpPlaybackRate()
    }

    override fun rMPMediaTimeChangeNotification(): RpcResponse {
        return RpcResponse()
    }
}