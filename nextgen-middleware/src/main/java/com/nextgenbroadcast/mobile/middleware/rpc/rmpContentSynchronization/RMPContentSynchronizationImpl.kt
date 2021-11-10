package com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization

import com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization.model.RmpMediaTime
import com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization.model.RmpPlaybackRate
import com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization.model.RmpPlaybackState
import com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization.model.RmpWallClockTime
import com.nextgenbroadcast.mobile.middleware.server.IApplicationSession

class RMPContentSynchronizationImpl(
    private val session: IApplicationSession
) : IRMPContentSynchronization {
    override fun queryRMPMediaTime(): RmpMediaTime {
        return RmpMediaTime()
    }

    override fun queryRMPWallClock(): RmpWallClockTime {
        return RmpWallClockTime()
    }

    override fun queryRMPPlaybackState(): RmpPlaybackState {
        return RmpPlaybackState(
            session.getParam(IApplicationSession.Params.PlaybackState)?.toIntOrNull() ?: 2 /* IDLE */
        )
    }

    override fun queryRMPPlaybackRate(): RmpPlaybackRate {
        return RmpPlaybackRate()
    }
}