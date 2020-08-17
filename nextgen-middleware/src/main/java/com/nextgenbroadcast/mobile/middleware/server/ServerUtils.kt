package com.nextgenbroadcast.mobile.middleware.server

import com.nextgenbroadcast.mobile.core.md5
import com.nextgenbroadcast.mobile.middleware.settings.IClientSettings

object ServerUtils {
    fun getUrl(entryPageUrl: String, appContextId: String, settings: IClientSettings, withSocket: Boolean): String {
        val contextPath = appContextId.md5()
        val appEntryPage = "https://${settings.hostName}:${settings.httpsPort}/$contextPath/$entryPageUrl"

        return if (!withSocket) appEntryPage else {
            "$appEntryPage?wsURL=wss://${settings.hostName}:${settings.wssPort}&rev=${ServerConstants.REVISION}"
        }
    }
}