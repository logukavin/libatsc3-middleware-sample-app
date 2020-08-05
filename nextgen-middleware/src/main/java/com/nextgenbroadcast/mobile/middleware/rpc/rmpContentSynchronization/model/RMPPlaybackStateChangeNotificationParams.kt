package com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization.model

data class RMPPlaybackStateChangeNotificationParams (
    var msgType: String? = null,
    var playbackState: Int? = null
)