package org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization;

import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model.RmpMediaTime;
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.model.RmpWallClockTime;

public class RMPContentSynchronizationImpl implements IRMPContentSynchronization {

    @Override
    public RmpMediaTime queryRMPMediaTime() {
        return null;
    }

    @Override
    public RmpWallClockTime queryRMPWallClock() {
        return null;
    }

    @Override
    public RmpWallClockTime queryRMPPlaybackState() {
        return null;
    }

    @Override
    public RmpWallClockTime queryRMPPlaybackRate() {
        return null;
    }

    @Override
    public RmpWallClockTime rMPMediaTimeChangeNotification() {
        return null;
    }
}
