package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

public class RPCManager {
    private static RPCManager instance;
private TempActivityCallback callback;
    private RPCManager(TempActivityCallback callback) {
        this.callback = callback;

    }
    public static RPCManager newInstance(TempActivityCallback callback) {
        if (instance == null){
            instance = new RPCManager(callback);
        }
        return instance;
    }

    public void updateViewPosition(Double scaleFactor, Double xPos, Double yPos) {
        callback.updateViewPosition(scaleFactor,xPos,yPos);
    }
}
