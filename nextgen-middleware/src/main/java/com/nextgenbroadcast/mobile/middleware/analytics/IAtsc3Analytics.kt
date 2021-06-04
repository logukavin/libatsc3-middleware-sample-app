package com.nextgenbroadcast.mobile.middleware.analytics

import android.location.Location

internal interface IAtsc3Analytics {
    fun setReportServerUrl(serverUrl: String?)

    fun startSession(bsid: Int, serviceId: Int, globalServiceId: String?, serviceType: Int)
    fun finishSession()
    fun startDisplayMediaContent()
    fun finishDisplayMediaContent()
    fun startApplicationSession()
    fun finishApplicationSession()

    fun getLocation(): Location
}