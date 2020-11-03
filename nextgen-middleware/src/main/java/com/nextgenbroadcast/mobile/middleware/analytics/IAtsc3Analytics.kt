package com.nextgenbroadcast.mobile.middleware.analytics

interface IAtsc3Analytics {
    fun startSession(bsid: Int, serviceId: Int, globalServiceId: String?, serviceType: Int)
    fun finishSession()
    fun startDisplayMediaContent()
    fun finishDisplayMediaContent()
    fun startApplicationSession()
    fun finishApplicationSession()
}