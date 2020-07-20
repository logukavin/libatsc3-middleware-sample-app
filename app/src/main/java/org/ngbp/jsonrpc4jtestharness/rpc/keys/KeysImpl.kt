package org.ngbp.jsonrpc4jtestharness.rpc.keys

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse
import org.ngbp.jsonrpc4jtestharness.rpc.keys.model.Keys

class KeysImpl() : IKeys {
    override fun requestKeys(listOfKeys: List<String>): Keys {
        return Keys().apply {
            this.accepted = listOfKeys
        }
    }

    override fun relinquishKeys(listOfKeys: List<String>): RpcResponse {
        return RpcResponse()
    }
}