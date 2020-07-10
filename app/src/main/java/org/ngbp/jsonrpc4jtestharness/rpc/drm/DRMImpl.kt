package org.ngbp.jsonrpc4jtestharness.rpc.drm

import org.ngbp.jsonrpc4jtestharness.rpc.drm.model.DRMOperation
import org.ngbp.jsonrpc4jtestharness.rpc.drm.model.NotifyParams

class DRMImpl : IDRM {
    override fun drmNotification(): NotifyParams {
        return NotifyParams()
    }

    override fun drmOperation(): DRMOperation {
        return DRMOperation()
    }
}