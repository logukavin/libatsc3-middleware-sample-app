package org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model.RmpMediaTime;
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model.RmpWallClockTime;

@JsonRpcService("")
public interface IRMPContentSynchronization {

    @JsonRpcMethod("org.atsc.query.rmpMediaTime")
    RmpMediaTime queryRMPMediaTime();

    @JsonRpcMethod("org.atsc.query.rmpMediaTime")
    RmpWallClockTime queryRMPWallClock();

    @JsonRpcMethod("org.atsc.query.rmpPlaybackState")
    RmpWallClockTime queryRMPPlaybackState();

    @JsonRpcMethod("org.atsc.query.rmpPlaybackRate")
    RmpWallClockTime queryRMPPlaybackRate();

    @JsonRpcMethod("org.atsc.notify")
    RmpWallClockTime rMPMediaTimeChangeNotification();

}
