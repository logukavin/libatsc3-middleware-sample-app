package com.nextgenbroadcast.mobile.middleware.analytics

class AppInterval(
        var appId: String?,
        var startTime: Long?,
        var endTime: Long?,
        var lifeCycle: Int?,
) {
    companion object {
        const val DOWNLOADED_AND_NOT_LAUNCHED = 1
        const val DOWNLOADED_AND_AUTO_LAUNCHED = 2
        const val DOWNLOADED_AND_USER_LAUNCHED = 3
    }
}