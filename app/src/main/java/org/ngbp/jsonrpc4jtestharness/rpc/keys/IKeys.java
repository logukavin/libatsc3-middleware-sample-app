package org.ngbp.jsonrpc4jtestharness.rpc.keys;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.model.GetFilterCodes;
import org.ngbp.jsonrpc4jtestharness.rpc.keys.model.Keys;

@JsonRpcService("")
public interface IKeys {
    @JsonRpcMethod("org.atsc.request.keys")
    Keys requestKeys();

    @JsonRpcMethod("org.atsc.relinquish.keys")
    Object relinquishKeys();

    @JsonRpcMethod("org.atsc.notify")
    Object requestKeysTimeout();
}
