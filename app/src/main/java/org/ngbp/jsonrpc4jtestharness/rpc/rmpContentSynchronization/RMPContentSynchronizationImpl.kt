package org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization

import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model.RmpMediaTime
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model.RmpWallClockTime

class RMPContentSynchronizationImpl : IRMPContentSynchronization {
    override fun queryRMPMediaTime(): RmpMediaTime? {
        return null
    }

    override fun queryRMPWallClock(): RmpWallClockTime? {
        return null
    }

    override fun queryRMPPlaybackState(): RmpWallClockTime? {
        return null
    }

    override fun queryRMPPlaybackRate(): RmpWallClockTime? {
        return null
    }

    override fun rMPMediaTimeChangeNotification(): RmpWallClockTime? {
        return null
    }
}