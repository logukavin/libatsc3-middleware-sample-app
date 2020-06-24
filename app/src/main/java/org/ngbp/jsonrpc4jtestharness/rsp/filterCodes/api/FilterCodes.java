package org.ngbp.jsonrpc4jtestharness.rsp.filterCodes.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rsp.filterCodes.model.GetFilterCodes;

@JsonRpcService("")
public interface FilterCodes {

    @JsonRpcMethod("org.atsc.getFilterCodes")
    GetFilterCodes getFilterCodes();

    @JsonRpcMethod("org.atsc.setFilterCodes")
    JsonRpcResponse<Object> setFilterCodes();
}
