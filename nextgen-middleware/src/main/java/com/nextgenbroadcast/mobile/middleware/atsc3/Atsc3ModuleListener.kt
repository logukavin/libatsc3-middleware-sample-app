package com.nextgenbroadcast.mobile.middleware.atsc3

import com.nextgenbroadcast.mobile.core.atsc3.MediaUrl
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.alerts.AeaTable
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.held.Atsc3HeldPackage
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.service.Atsc3Service

interface Atsc3ModuleListener {
    fun onStateChanged(state: Atsc3ModuleState)
    fun onConfigurationChanged(index: Int, count: Int)

    fun onApplicationPackageReceived(appPackage: Atsc3Application)

    fun onServiceLocationTableChanged(services: List<Atsc3Service>, reportServerUrl: String?)
    fun onServicePackageChanged(pkg: Atsc3HeldPackage?)
    fun onServiceMediaReady(mediaUrl: MediaUrl, delayBeforePlayMs: Long)
    fun onServiceGuideUnitReceived(filePath: String)

    fun onError(message: String)
    fun onAeatTableChanged(list: List<AeaTable>)
}