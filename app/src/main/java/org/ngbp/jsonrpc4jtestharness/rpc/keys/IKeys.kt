package org.ngbp.jsonrpc4jtestharness.rpc.keys

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.keys.model.Keys

@JsonRpcType
interface IKeys {
    @JsonRpcMethod("org.atsc.request.keys")
    fun requestKeys(): Keys?

    @JsonRpcMethod("org.atsc.relinquish.keys")
    fun relinquishKeys(): Any?

    @JsonRpcMethod("org.atsc.notify")
    fun requestKeysTimeout(): Any?
}