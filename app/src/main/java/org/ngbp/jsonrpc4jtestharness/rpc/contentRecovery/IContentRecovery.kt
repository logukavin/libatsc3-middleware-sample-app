package org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse
import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model.CecoveredComponentInfo
import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model.ContentRecoveryState

@JsonRpcType
interface IContentRecovery {
    @JsonRpcMethod("org.atsc.query.contentRecoveryState")
    fun queryContentRecoveryState(): ContentRecoveryState

    @JsonRpcMethod("org.atsc.query.recoveredComponentInfo")
    fun queryRecoveredComponentInfo(): CecoveredComponentInfo

    @JsonRpcMethod("org.atsc.notify")
    fun contentRecoveryStateChangeNotification(): CecoveredComponentInfo
}