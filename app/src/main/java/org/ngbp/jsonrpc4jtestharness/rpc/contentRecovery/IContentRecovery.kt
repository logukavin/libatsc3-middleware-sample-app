package org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model.ContentRecoveryState
import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model.RecoveredComponentInfo

@JsonRpcType
interface IContentRecovery {
    @JsonRpcMethod("org.atsc.query.contentRecoveryState")
    fun queryContentRecoveryState(): ContentRecoveryState

    @JsonRpcMethod("org.atsc.query.recoveredComponentInfo")
    fun queryRecoveredComponentInfo(): RecoveredComponentInfo
}