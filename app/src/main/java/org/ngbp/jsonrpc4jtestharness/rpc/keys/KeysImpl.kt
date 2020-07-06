package org.ngbp.jsonrpc4jtestharness.rpc.keys

import org.ngbp.jsonrpc4jtestharness.rpc.keys.model.Keys

class KeysImpl : IKeys {
    override fun requestKeys(): Keys? {
        return null
    }

    override fun relinquishKeys(): Any? {
        return null
    }

    override fun requestKeysTimeout(): Any? {
        return null
    }
}