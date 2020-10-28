package com.nextgenbroadcast.mobile.middleware.analytics

class AVService(
        val country: String,
        val bsid: String?,
        val serviceID: Int,
        val globalServiceID: String?,
        val serviceType: Int
) {
    val reportIntervalList = mutableListOf<ReportInterval>()
}