package org.ngbp.jsonrpc4jtestharness.rpc.filterCodes;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.model.GetFilterCodes;

@JsonRpcService("")
public interface IFilterCodes {

    @JsonRpcMethod("org.atsc.getFilterCodes")
    GetFilterCodes getFilterCodes();

    @JsonRpcMethod("org.atsc.setFilterCodes")
    Object setFilterCodes();
}
