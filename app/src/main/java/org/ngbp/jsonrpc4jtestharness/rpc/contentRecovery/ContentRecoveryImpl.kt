package org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery

import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model.ContentRecoveryState
import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model.RecoveredComponentInfo

class ContentRecoveryImpl : IContentRecovery {
    override fun queryContentRecoveryState(): ContentRecoveryState {
        return ContentRecoveryState()
    }

    override fun queryRecoveredComponentInfo(): RecoveredComponentInfo {
        return RecoveredComponentInfo()
    }
}