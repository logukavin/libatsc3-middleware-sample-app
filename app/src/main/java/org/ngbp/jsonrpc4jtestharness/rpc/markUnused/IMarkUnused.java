package org.ngbp.jsonrpc4jtestharness.rpc.markUnused;


import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;


@JsonRpcType
public interface IMarkUnused {

    @JsonRpcMethod("org.atsc.cache.markUnused")
    Object markUnused();
}
