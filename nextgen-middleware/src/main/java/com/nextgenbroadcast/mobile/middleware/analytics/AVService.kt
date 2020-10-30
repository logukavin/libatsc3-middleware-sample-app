package com.nextgenbroadcast.mobile.middleware.analytics

class AVService(
        val country: String,
        val bsid: Int,
        val serviceID: Int,
        val globalServiceID: String?,
        val serviceType: Int
) {
    val reportIntervals = mutableListOf<ReportInterval>()
    val appIntervals = mutableListOf<AppInterval>()
}