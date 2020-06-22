package org.ngbp.jsonrpc4jtestharness.rsp.rmpContentSynchronization.api;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rsp.rmpContentSynchronization.model.RmpMediaTime;
import org.ngbp.jsonrpc4jtestharness.rsp.rmpContentSynchronization.model.RmpWallClockTime;

public class RMPContentSynchronizationImpl implements RMPContentSynchronization {
    @Override
    public JsonRpcResponse<RmpMediaTime> queryRMPMediaTime() {
        return null;
    }

    @Override
    public JsonRpcResponse<RmpWallClockTime> queryRMPWallClock() {
        return null;
    }

    @Override
    public JsonRpcResponse<RmpWallClockTime> queryRMPPlaybackState() {
        return null;
    }

    @Override
    public JsonRpcResponse<RmpWallClockTime> queryRMPPlaybackRate() {
        return null;
    }

    @Override
    public JsonRpcResponse<RmpWallClockTime> rMPMediaTimeChangeNotification() {
        return null;
    }
}
