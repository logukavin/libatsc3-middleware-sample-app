package com.nextgenbroadcast.mobile.middleware.analytics

import java.util.*

class ReportInterval(
        var startTime: Date,
        var endTime: Date?,
        var destinationDeviceType: Int?
) {
    val broadcastIntervals = mutableListOf<BroadcastInterval>()
    val appIntervals = mutableListOf<AppInterval>()

    val isFinished: Boolean
        get() {
            return endTime != null
        }
}