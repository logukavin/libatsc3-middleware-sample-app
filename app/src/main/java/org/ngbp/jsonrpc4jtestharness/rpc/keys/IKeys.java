package org.ngbp.jsonrpc4jtestharness.rpc.keys;

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;

import org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.model.GetFilterCodes;
import org.ngbp.jsonrpc4jtestharness.rpc.keys.model.Keys;

@JsonRpcType
public interface IKeys {
    @JsonRpcMethod("org.atsc.request.keys")
    Keys requestKeys();

    @JsonRpcMethod("org.atsc.relinquish.keys")
    Object relinquishKeys();

    @JsonRpcMethod("org.atsc.notify")
    Object requestKeysTimeout();
}
