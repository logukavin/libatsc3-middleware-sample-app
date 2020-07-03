package org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions;

import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCManager;
import org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.model.AudioVolume;
import org.ngbp.jsonrpc4jtestharness.rpc.EmptyModel;

public class ReceiverActionImpl implements IReceiverAction {
    private RPCManager rpcManager;

    public ReceiverActionImpl(RPCManager rpcManager) {
        this.rpcManager = rpcManager;
    }

    @Override
    public Object acquireService() {
        return null;
    }

    @Override
    public EmptyModel videoScalingAndPositioning(Double scaleFactor, Double xPos, Double yPos) {
        rpcManager.updateViewPosition(scaleFactor, xPos, yPos);
        return new EmptyModel();
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
