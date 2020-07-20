package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.ServiceGuideUrls

data class ServiceGuideChangeNotification (
    var msgType: String? = null,
    var urlList: MutableList<ServiceGuideUrls?>? = null
)