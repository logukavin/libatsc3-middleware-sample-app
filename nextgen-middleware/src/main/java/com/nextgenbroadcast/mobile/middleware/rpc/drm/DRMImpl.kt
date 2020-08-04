package com.nextgenbroadcast.mobile.middleware.rpc.drm

import com.nextgenbroadcast.mobile.middleware.rpc.drm.model.DRMOperation

class DRMImpl : IDRM {
    override fun drmOperation(): DRMOperation {
        return DRMOperation()
    }
}