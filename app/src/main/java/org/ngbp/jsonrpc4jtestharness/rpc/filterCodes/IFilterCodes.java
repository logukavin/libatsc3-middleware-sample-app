package org.ngbp.jsonrpc4jtestharness.rpc.filterCodes;

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;

import org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.model.GetFilterCodes;

@JsonRpcType
public interface IFilterCodes {

    @JsonRpcMethod("org.atsc.getFilterCodes")
    GetFilterCodes getFilterCodes();

    @JsonRpcMethod("org.atsc.setFilterCodes")
    Object setFilterCodes();
}
