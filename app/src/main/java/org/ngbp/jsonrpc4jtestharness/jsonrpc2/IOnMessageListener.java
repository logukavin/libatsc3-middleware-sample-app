package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

public interface IOnMessageListener {
    public void onMessageReceiver(String message);
    public void onMessageReceiver(byte[] message);
}
