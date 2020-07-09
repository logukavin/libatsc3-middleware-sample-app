package org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse
import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model.CecoveredComponentInfo
import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model.ContentRecoveryState

class ContentRecoveryImpl : IContentRecovery {
    override fun queryContentRecoveryState(): ContentRecoveryState {
        return ContentRecoveryState()
    }

    override fun queryRecoveredComponentInfo(): CecoveredComponentInfo {
        return CecoveredComponentInfo()
    }

    override fun contentRecoveryStateChangeNotification(): CecoveredComponentInfo {
        return CecoveredComponentInfo()
    }
}