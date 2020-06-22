package org.ngbp.jsonrpc4jtestharness.rsp.contentRecovery.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rsp.contentRecovery.model.CecoveredComponentInfo;
import org.ngbp.jsonrpc4jtestharness.rsp.contentRecovery.model.ContentRecoveryState;
import org.ngbp.jsonrpc4jtestharness.rsp.mapper.JsonRpcResponse;

@JsonRpcService("")
public interface ContentRecovery {

    @JsonRpcMethod("org.atsc.query.contentRecoveryState")
    JsonRpcResponse<ContentRecoveryState> queryContentRecoveryState();

    @JsonRpcMethod("org.atsc.query.recoveredComponentInfo")
    JsonRpcResponse<CecoveredComponentInfo> queryRecoveredComponentInfo ();

    @JsonRpcMethod("org.atsc.notify")
    JsonRpcResponse<CecoveredComponentInfo> contentRecoveryStateChangeNotification ();
}
