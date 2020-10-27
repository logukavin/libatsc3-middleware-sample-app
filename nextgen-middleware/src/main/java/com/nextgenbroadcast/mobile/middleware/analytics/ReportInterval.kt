package com.nextgenbroadcast.mobile.middleware.analytics

class ReportInterval(
        var startTime: Long,
        var endTime: Long?,
        var destinationDeviceType: String?,
        var contentID: ContentID? = null,
        var component: Component? = null
) {

    val broadcastIntervals = mutableListOf<BroadcastInterval>()
    val appIntervals = mutableListOf<AppInterval>()

    companion object {
        const val PRESENTED_ON_PRIMARY_DEVICE = 0
        const val PRESENTED_ON_COMPANION_DEVICE = 1
        const val SENT_TO_TIME_SHIFT_BUFFER = 2
        const val SENT_TO_LONG_TERM_STORAGE = 3
        //4 to 255 – Reserved
    }
}