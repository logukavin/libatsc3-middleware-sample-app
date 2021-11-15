package com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization

import com.nextgenbroadcast.mobile.middleware.rpc.RpcException
import com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization.model.RmpMediaTime
import com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization.model.RmpPlaybackRate
import com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization.model.RmpPlaybackState
import com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization.model.RmpWallClockTime
import com.nextgenbroadcast.mobile.middleware.server.IApplicationSession

class RMPContentSynchronizationImpl(
    private val session: IApplicationSession
) : IRMPContentSynchronization {
    override fun queryRMPMediaTime(): RmpMediaTime {
        val playbackTime = session.getParam(IApplicationSession.Params.PlaybackTime)?.toDouble() ?: 0.0
        return RmpMediaTime(
            playbackTime / 1000
        )
    }

    override fun queryRMPWallClock(): RmpWallClockTime {
        throw RpcException()
        //return RmpWallClockTime()
    }

    override fun queryRMPPlaybackState(): RmpPlaybackState {
        val playbackState = session.getParam(IApplicationSession.Params.PlaybackState)?.toIntOrNull()
        return RmpPlaybackState(
            playbackState ?: -1 /* IDLE */
        )
    }

    override fun queryRMPPlaybackRate(): RmpPlaybackRate {
        return RmpPlaybackRate(1f)
    }
}