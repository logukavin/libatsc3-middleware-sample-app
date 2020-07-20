package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.cta708
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.imsc1

data class CaptionDisplayPreferencesChangeNotification (
    var msgType: String? = null,
    var cta708: cta708? = null,
    var imsc1: imsc1? = null
)