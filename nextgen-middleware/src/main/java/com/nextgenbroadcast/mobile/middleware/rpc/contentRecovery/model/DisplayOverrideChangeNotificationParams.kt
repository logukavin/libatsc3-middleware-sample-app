package com.nextgenbroadcast.mobile.middleware.rpc.contentRecovery.model

data class DisplayOverrideChangeNotificationParams (
    var msgType: String? = null,
    var resourceBlocking: Boolean? = null,
    var displayOverride: Boolean? = null
)