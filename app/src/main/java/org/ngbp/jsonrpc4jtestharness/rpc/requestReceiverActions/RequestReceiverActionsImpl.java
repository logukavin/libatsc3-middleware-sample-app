package org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions;

import android.util.Log;

import org.ngbp.jsonrpc4jtestharness.jsonrpc2.RPCManager;
import org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.model.AudioVolume;

public class RequestReceiverActionsImpl implements IRequestReceiverActions {
    private RPCManager rpcManager;
    public RequestReceiverActionsImpl(RPCManager rpcManager) {
        this.rpcManager = rpcManager;
    }

    @Override
    public Object acquireService() {
        return null;
    }

    @Override
    public Object videoScalingAndPositioning(Double scaleFactor, Double xPos, Double yPos) {
        Log.d("videoScalingAndPositioning ","");
        rpcManager.updateViewPosition(scaleFactor,xPos,yPos);
        return new Object();
    }

    @Override
    public Object setRMPURL() {
        return null;
    }

    @Override
    public AudioVolume audioVolume() {
        return null;
    }
}
