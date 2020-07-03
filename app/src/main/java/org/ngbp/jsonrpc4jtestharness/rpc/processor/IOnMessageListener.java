package org.ngbp.jsonrpc4jtestharness.rpc.processor;

public interface IOnMessageListener {
    public void onMessageReceiver(String message);

    public void onMessageReceiver(byte[] message);
}
