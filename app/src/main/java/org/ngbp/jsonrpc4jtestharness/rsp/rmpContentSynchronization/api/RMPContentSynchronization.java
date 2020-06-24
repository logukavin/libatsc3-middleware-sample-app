package org.ngbp.jsonrpc4jtestharness.rsp.rmpContentSynchronization.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rsp.rmpContentSynchronization.model.RmpMediaTime;
import org.ngbp.jsonrpc4jtestharness.rsp.rmpContentSynchronization.model.RmpWallClockTime;

@JsonRpcService("")
public interface RMPContentSynchronization {

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
