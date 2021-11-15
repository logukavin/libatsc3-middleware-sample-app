package com.nextgenbroadcast.mobile.middleware.gateway

import com.nextgenbroadcast.mobile.middleware.atsc3.ISignalingData
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl

interface IApplicationInterface {
    fun requestRMPPosition(scaleFactor: Double, xPos: Double, yPos: Double)

    fun requestMediaPlay(mediaUrl: String? = null, delay: Long)
    fun requestMediaStop(delay: Long)

    fun requestFileCache(baseUrl: String?, rootPath: String?, paths: List<String>, filters: List<String>?): Boolean

    fun getServiceGuideUrls(service: String?): List<SGUrl>

    fun requestServiceChange(globalServiceId: String): Boolean

    fun getAEATChangingList(): List<String>

    fun getSignalingInfo(names: List<String>): List<ISignalingData>
}