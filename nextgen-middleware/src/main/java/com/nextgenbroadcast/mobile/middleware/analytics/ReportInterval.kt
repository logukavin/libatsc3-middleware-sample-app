package com.nextgenbroadcast.mobile.middleware.analytics

class ReportInterval(
        var startTime: Long,
        var endTime: Long?,
        var destinationDeviceType: Int?
) {
    val broadcastIntervals = mutableListOf<BroadcastInterval>()
    val appIntervals = mutableListOf<AppInterval>()

    val isFinished: Boolean
        get() {
            return endTime != null
        }
}