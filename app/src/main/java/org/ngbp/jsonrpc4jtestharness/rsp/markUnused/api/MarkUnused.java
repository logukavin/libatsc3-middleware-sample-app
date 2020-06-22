package org.ngbp.jsonrpc4jtestharness.rsp.markUnused.api;


import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;


@JsonRpcService("")
public interface MarkUnused {

    @JsonRpcMethod("org.atsc.cache.markUnused")
    JsonRpcResponse<Object> markUnused();
}
