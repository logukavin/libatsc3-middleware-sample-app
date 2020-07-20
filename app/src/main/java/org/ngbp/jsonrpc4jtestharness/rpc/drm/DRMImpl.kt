package org.ngbp.jsonrpc4jtestharness.rpc.drm

import org.ngbp.jsonrpc4jtestharness.rpc.drm.model.DRMOperation

class DRMImpl : IDRM {
    override fun drmOperation(): DRMOperation {
        return DRMOperation()
    }
}