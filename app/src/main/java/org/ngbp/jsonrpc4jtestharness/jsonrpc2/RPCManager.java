package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

public class RPCManager {
    private static RPCManager instance;
    private TempActivityCallback callback;

    public RPCManager() {
    }

    public TempActivityCallback getCallback() {
        return callback;
    }

    public void setCallback(TempActivityCallback callback) {
        this.callback = callback;
    }

    public void updateViewPosition(Double scaleFactor, Double xPos, Double yPos) {
        callback.updateViewPosition(scaleFactor, xPos, yPos);
    }
}
