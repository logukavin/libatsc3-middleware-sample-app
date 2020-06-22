package org.ngbp.jsonrpc4jtestharness.rsp.rmpContentSynchronization.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rsp.mapper.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rsp.rmpContentSynchronization.model.RmpMediaTime;
import org.ngbp.jsonrpc4jtestharness.rsp.rmpContentSynchronization.model.RmpWallClockTime;

@JsonRpcService("")
public interface RMPContentSynchronization {

    @JsonRpcMethod("org.atsc.query.rmpMediaTime")
    JsonRpcResponse<RmpMediaTime> queryRMPMediaTime();

    @JsonRpcMethod("org.atsc.query.rmpMediaTime")
    JsonRpcResponse<RmpWallClockTime> queryRMPWallClock();

    @JsonRpcMethod("org.atsc.query.rmpPlaybackState")
    JsonRpcResponse<RmpWallClockTime> queryRMPPlaybackState();

    @JsonRpcMethod("org.atsc.query.rmpPlaybackRate")
    JsonRpcResponse<RmpWallClockTime> queryRMPPlaybackRate();

    @JsonRpcMethod("org.atsc.notify")
    JsonRpcResponse<RmpWallClockTime> rMPMediaTimeChangeNotification();

}
