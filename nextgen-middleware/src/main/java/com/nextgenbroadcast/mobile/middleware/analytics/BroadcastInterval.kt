package com.nextgenbroadcast.mobile.middleware.analytics

import java.util.*

class BroadcastInterval(
        var broadcastStartTime: Date? = null,
        var broadcastEndTime: Date? = null,
        var receiverStartTime: Date
)