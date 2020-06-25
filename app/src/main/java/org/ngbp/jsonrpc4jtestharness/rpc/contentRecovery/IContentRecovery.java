package org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model.CecoveredComponentInfo;
import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model.ContentRecoveryState;

@JsonRpcService("")
public interface IContentRecovery {

    @JsonRpcMethod("org.atsc.query.contentRecoveryState")
    ContentRecoveryState queryContentRecoveryState();

    @JsonRpcMethod("org.atsc.query.recoveredComponentInfo")
    CecoveredComponentInfo queryRecoveredComponentInfo();

    @JsonRpcMethod("org.atsc.notify")
    CecoveredComponentInfo contentRecoveryStateChangeNotification();
}
