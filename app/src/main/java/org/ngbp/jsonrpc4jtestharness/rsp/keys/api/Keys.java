package org.ngbp.jsonrpc4jtestharness.rsp.keys.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rsp.filterCodes.model.GetFilterCodes;
import org.ngbp.jsonrpc4jtestharness.rsp.mapper.JsonRpcResponse;

@JsonRpcService("")
public interface Keys {
    @JsonRpcMethod("org.atsc.request.keys")
    JsonRpcResponse<GetFilterCodes> requestKeys();

    @JsonRpcMethod("org.atsc.relinquish.keys")
    JsonRpcResponse<Object> relinquishKeys();

    @JsonRpcMethod("org.atsc.notify")
    JsonRpcResponse<Object> requestKeysTimeout();
}
