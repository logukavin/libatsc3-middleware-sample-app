package com.nextgenbroadcast.mobile.middleware.analytics.model

import com.google.gson.annotations.SerializedName

class AVService(
        val country: String,
        val bsid: Int,
        val serviceID: Int,
        val globalServiceID: String?,
        val serviceType: Int
) {
    @SerializedName("reportInterval")
    val reportIntervals = mutableListOf<ReportInterval>()
}