package com.nextgenbroadcast.mobile.middleware.rpc.notification.model

import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.VideoDescriptionService

data class AudioAccessibilityPreferenceChangeNotification (
    var msgType: String? = null,
    var videoDescriptionService: VideoDescriptionService? = null
)