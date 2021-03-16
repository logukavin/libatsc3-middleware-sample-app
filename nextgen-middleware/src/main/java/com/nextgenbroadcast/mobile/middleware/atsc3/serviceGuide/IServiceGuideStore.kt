package com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide

import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.unit.SGContent
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.unit.SGService
import java.util.concurrent.ConcurrentHashMap

internal interface IServiceGuideStore {
    fun subscribe(notifyUpdated: () -> Unit)
    fun clearAll()
    fun storeService(serviceMap: Map<Int, SGService>)
    fun storeContent(contentMap: ConcurrentHashMap<String, SGContent>)
}