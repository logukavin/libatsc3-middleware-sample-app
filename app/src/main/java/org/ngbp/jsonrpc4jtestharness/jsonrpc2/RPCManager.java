package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

public class RPCManager {
    private static RPCManager instance;

    private RPCManager() {

    }
    public static RPCManager newInstance() {
        if (instance == null){
            instance = new RPCManager();
        }
        return instance;
    }
}
