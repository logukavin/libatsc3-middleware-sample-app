package org.ngbp.jsonrpc4jtestharness.rpc.keys

import org.ngbp.jsonrpc4jtestharness.rpc.keys.model.Keys
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCManager

class KeysImpl(val rpcManager: RPCManager) : IKeys {
    override fun requestKeys(listOfKeys: List<String>): Keys {
        return Keys().apply {
            this.accepted = rpcManager.keysList
        }
    }

    override fun relinquishKeys(): Any? {
        return null
    }

    override fun requestKeysTimeout(): Any? {
        return null
    }
}