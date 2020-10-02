package com.nextgenbroadcast.mobile.middleware.rpc.notification.model

import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.cta708
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.imsc1

data class CaptionDisplayPreferencesChangeNotification (
    var msgType: String? = null,
    var cta708: cta708? = null,
    var imsc1: imsc1? = null
)