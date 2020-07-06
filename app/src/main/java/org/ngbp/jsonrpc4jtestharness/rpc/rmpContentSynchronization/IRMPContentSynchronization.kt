package org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model.RmpMediaTime
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model.RmpWallClockTime

@JsonRpcType
interface IRMPContentSynchronization {
    @JsonRpcMethod("org.atsc.query.rmpMediaTime")
    fun queryRMPMediaTime(): RmpMediaTime?

    @JsonRpcMethod("org.atsc.query.rmpMediaTime")
    fun queryRMPWallClock(): RmpWallClockTime?

    @JsonRpcMethod("org.atsc.query.rmpPlaybackState")
    fun queryRMPPlaybackState(): RmpWallClockTime?

    @JsonRpcMethod("org.atsc.query.rmpPlaybackRate")
    fun queryRMPPlaybackRate(): RmpWallClockTime?

    @JsonRpcMethod("org.atsc.notify")
    fun rMPMediaTimeChangeNotification(): RmpWallClockTime?
}