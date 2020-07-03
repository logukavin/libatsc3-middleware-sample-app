package org.ngbp.jsonrpc4jtestharness.rpc.processor

interface IOnMessageListener {
    fun onMessageReceiver(message: String?)
    fun onMessageReceiver(message: ByteArray?)
}