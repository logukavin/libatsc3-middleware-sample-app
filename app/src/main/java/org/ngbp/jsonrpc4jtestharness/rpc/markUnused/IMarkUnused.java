package org.ngbp.jsonrpc4jtestharness.rpc.markUnused;


import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;


@JsonRpcService("")
public interface IMarkUnused {

    @JsonRpcMethod("org.atsc.cache.markUnused")
    Object markUnused();
}
