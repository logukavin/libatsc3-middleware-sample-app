package com.nextgenbroadcast.mobile.middleware.rpc.notification.model

data class ContentChangeNotification (
    var msgType: String? = null,
    var packageList: MutableList<String?>? = null
)