package org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery;

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;

import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model.CecoveredComponentInfo;
import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model.ContentRecoveryState;

@JsonRpcType
public interface IContentRecovery {

    @JsonRpcMethod("org.atsc.query.contentRecoveryState")
    ContentRecoveryState queryContentRecoveryState();

    @JsonRpcMethod("org.atsc.query.recoveredComponentInfo")
    CecoveredComponentInfo queryRecoveredComponentInfo();

    @JsonRpcMethod("org.atsc.notify")
    CecoveredComponentInfo contentRecoveryStateChangeNotification();
}
