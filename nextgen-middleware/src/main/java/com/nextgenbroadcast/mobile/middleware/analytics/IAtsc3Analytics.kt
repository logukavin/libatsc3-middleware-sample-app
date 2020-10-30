package com.nextgenbroadcast.mobile.middleware.analytics

interface IAtsc3Analytics {
    fun startSession(bsid: Int, serviceId: Int, globalServiceId: String?, serviceType: Int)
    fun startDisplayContent()
    fun finishDisplayContent()
    fun startBASession(appId: String)
    fun finishBASession()
    fun finishSession()
}