package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

public class RPCManager {
    private static RPCManager instance;
    private ReceiverActionCallback callback;

    public RPCManager() {
    }

    public ReceiverActionCallback getCallback() {
        return callback;
    }

    public void setCallback(ReceiverActionCallback callback) {
        this.callback = callback;
    }

    public void updateViewPosition(Double scaleFactor, Double xPos, Double yPos) {
        callback.updateViewPosition(scaleFactor, xPos, yPos);
    }
}
